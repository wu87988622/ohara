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
import _ from 'lodash';
import PropTypes from 'prop-types';
import CloseIcon from '@material-ui/icons/Close';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';

import * as hooks from 'hooks';
import * as propertyUtils from './PipelinePropertyViewUtils';
import SettingsPanel from './SettingsPanel';
import NodePanel from './NodePanel';
import MetricsPanel from './MetricsPanel';
import { KIND, CELL_STATUS } from 'const';
import { Wrapper } from './PipelinePropertyViewStyles';
import { Dialog } from 'components/common/Dialog';

const PipelinePropertyView = props => {
  const { handleClose, element, pipelineObjects, isMetricsOn } = props;
  const topics = hooks.useTopicsInPipeline();
  const streams = hooks.useStreams();
  const connectors = [...hooks.useConnectors(), ...hooks.useShabondis()];

  const [isOpen, setIsOpen] = React.useState(false);
  const [tags, setTags] = React.useState({
    json: null,
    name: '',
  });

  if (!element) return null;
  const { name: cellName, displayName } = element;
  let settings;
  switch (element.kind) {
    case KIND.source:
    case KIND.sink:
      settings = connectors.find(connector => connector.name === cellName);
      break;
    case KIND.stream:
      settings = streams.find(stream => stream.name === cellName);
      break;
    case KIND.topic:
      settings = topics.find(topic => topic.name === cellName);
      break;
    default:
      break;
  }

  if (!settings?.settingDefinitions) return null;

  return (
    <Wrapper square variant="outlined">
      <div className="title-wrapper">
        <div className="title-info">
          <div
            className={`icon-wrapper ${_.get(
              settings,
              'state',
              CELL_STATUS.stopped,
            ).toLowerCase()}`}
          >
            {propertyUtils.renderIcon(element)}
          </div>
          <div className="title-text">
            <Typography variant="h5">{displayName}</Typography>
            <div className="status">
              <Typography
                className="status-key"
                color="textSecondary"
                component="span"
                variant="body2"
              >
                Status:
              </Typography>
              <Typography
                className="status-value"
                component="span"
                variant="body2"
              >
                {_.get(settings, 'state', CELL_STATUS.stopped)}
              </Typography>
            </div>
          </div>
        </div>
        <IconButton className="close-button" onClick={handleClose}>
          <CloseIcon />
        </IconButton>
      </div>

      <SettingsPanel
        setFullTagViewDialogOpen={setIsOpen}
        setTags={setTags}
        settings={settings}
      />
      {// For topic component, we should display its "partitionInfos" instead.
      // Please see #4677 for more info about this
      element.kind !== KIND.topic && (
        <NodePanel tasksStatus={settings.tasksStatus} />
      )}
      <MetricsPanel
        currentCellName={cellName}
        isMetricsOn={isMetricsOn}
        pipelineObjects={pipelineObjects}
      />

      <Dialog
        onClose={() => setIsOpen(false)}
        open={isOpen}
        showActions={false}
        title={`Full tags content of ${tags.name}`}
      >
        {tags.json ? (
          <pre>{JSON.stringify(JSON.parse(tags.json), null, 4)}</pre>
        ) : (
          ''
        )}
      </Dialog>
    </Wrapper>
  );
};

PipelinePropertyView.propTypes = {
  handleClose: PropTypes.func.isRequired,
  isMetricsOn: PropTypes.bool.isRequired,
  element: PropTypes.object,
  pipelineObjects: PropTypes.array,
};

export default PipelinePropertyView;
