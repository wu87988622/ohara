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
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import WavesIcon from '@material-ui/icons/Waves';
import StorageIcon from '@material-ui/icons/Storage';
import cx from 'classnames';
import Scrollbar from 'react-scrollbars-custom';

import * as hooks from 'hooks';
import { KIND } from 'const';
import { AddSharedTopicIcon } from 'components/common/Icon';

const Outline = ({ pipelineApi }) => {
  const selectedCell = hooks.useCurrentPipelineCell();

  const getIcon = (kind, isShared) => {
    const { source, sink, stream, topic } = KIND;

    if (kind === source) return <FlightTakeoffIcon />;
    if (kind === sink) return <FlightLandIcon />;
    if (kind === stream) return <WavesIcon />;
    if (kind === topic) {
      return isShared ? (
        <AddSharedTopicIcon width={20} height={22} />
      ) : (
        <StorageIcon />
      );
    }
  };

  return (
    <Scrollbar>
      <ul className="list">
        {pipelineApi &&
          pipelineApi.getElements().map(element => {
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
                onClick={() => pipelineApi.highlight(id)}
                key={id}
              >
                {getIcon(kind, isShared)}
                {isTopic && !isShared ? displayName : name}
              </li>
            );
          })}
      </ul>
    </Scrollbar>
  );
};

Outline.propTypes = {
  pipelineApi: PropTypes.shape({
    highlight: PropTypes.func.isRequired,
    getElements: PropTypes.func.isRequired,
  }),
};

export default Outline;
