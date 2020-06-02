/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import clx from 'classnames';
import Scrollbar from 'react-scrollbars-custom';
import ShareIcon from '@material-ui/icons/Share';
import Link from '@material-ui/core/Link';

import * as hooks from 'hooks';
import { Wrapper } from './PipelineListStyles';

const PipelineList = () => {
  const currentPipeline = hooks.usePipeline();
  const switchPipeline = hooks.useSwitchPipelineAction();
  const pipelines = hooks.usePipelines();

  return (
    <Scrollbar>
      <Wrapper>
        {pipelines.map((pipeline) => (
          <li key={pipeline.name}>
            <Link
              className={clx({
                'active-link': pipeline.name === currentPipeline?.name,
              })}
              onClick={() => {
                if (pipeline.name !== currentPipeline?.name) {
                  switchPipeline(pipeline.name);
                }
              }}
            >
              <ShareIcon className="link-icon" />
              {pipeline.name}
            </Link>
          </li>
        ))}
      </Wrapper>
    </Scrollbar>
  );
};

export default PipelineList;
