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
import PropTypes from 'prop-types';
import Scrollbar from 'react-scrollbars-custom';
import cx from 'classnames';

import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import WavesIcon from '@material-ui/icons/Waves';
import StorageIcon from '@material-ui/icons/Storage';
import ExtensionIcon from '@material-ui/icons/Extension';
import Typography from '@material-ui/core/Typography';

import * as hooks from 'hooks';
import { KIND } from 'const';
import { Wrapper } from './OutlineStyles';
import { AddSharedTopicIcon } from 'components/common/Icon';

const Outline = ({ pipelineApi, isExpanded }) => {
  const selectedCell = hooks.useCurrentPipelineCell();
  const currentPipeline = hooks.usePipeline();
  const elements = currentPipeline?.tags?.cells || [];

  const getIcon = (kind, isShared) => {
    const { source, sink, stream, topic } = KIND;

    if (kind === source) return <FlightTakeoffIcon />;
    if (kind === sink) return <FlightLandIcon />;
    if (kind === stream) return <WavesIcon />;
    if (kind === topic) {
      return isShared ? (
        <AddSharedTopicIcon height={22} width={20} />
      ) : (
        <StorageIcon />
      );
    }
  };

  if (!pipelineApi) return null;

  return (
    <Wrapper isExpanded={isExpanded}>
      <Typography variant="h5">
        <ExtensionIcon />
        Outline
      </Typography>
      <div className="scrollbar-wrapper">
        <Scrollbar>
          <ul className="list">
            {elements.map(element => {
              const { id, name, kind, isShared, displayName } = element;

              const isTopic = kind === KIND.topic;

              const className = cx({
                'is-selected': selectedCell?.name === element.name,
                'is-shared': isTopic && isShared,
                'pipeline-only': isTopic && !isShared,
                [kind]: kind,
              });

              return (
                <li
                  className={className}
                  key={id}
                  onClick={() => pipelineApi.highlight(id)}
                >
                  {getIcon(kind, isShared)}
                  {isTopic && !isShared ? displayName : name}
                </li>
              );
            })}
          </ul>
        </Scrollbar>
      </div>
    </Wrapper>
  );
};

Outline.propTypes = {
  pipelineApi: PropTypes.shape({
    highlight: PropTypes.func.isRequired,
  }),
  isExpanded: PropTypes.bool.isRequired,
};

export default Outline;
