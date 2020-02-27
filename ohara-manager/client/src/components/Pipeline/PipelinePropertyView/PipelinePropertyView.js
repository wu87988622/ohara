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

import React, { useState } from 'react';
import _ from 'lodash';
import PropTypes from 'prop-types';
import TuneIcon from '@material-ui/icons/Tune';
import CloseIcon from '@material-ui/icons/Close';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import StorageIcon from '@material-ui/icons/Storage';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import SignalCellularAltIcon from '@material-ui/icons/SignalCellularAlt';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import CodeIcon from '@material-ui/icons/Code';
import ArrowRightIcon from '@material-ui/icons/ArrowRight';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';
import PropertyField from './PipelinePropertyViewField';

import { KIND, CELL_STATUS } from 'const';
import { Wrapper } from './PipelinePropertyViewStyles';
import { Tooltip } from 'components/common/Tooltip';
import { Dialog } from 'components/common/Dialog';
import * as context from 'context';
import * as propertyUtils from './PipelinePropertyViewUtils';

const PipelinePropertyView = props => {
  const { handleClose, element, cellsMetrics } = props;
  const cellMetrics = cellsMetrics.find(cell => cell.name === element.name);
  const metrics = _.isUndefined(cellMetrics)
    ? { meters: [] }
    : cellMetrics.metrics;

  const { data: currentConnector } = context.useConnectorState();
  const { data: currentStream } = context.useStreamState();
  const { data: currentTopic } = context.useTopicState();
  const [isOpen, setIsOpen] = useState(false);
  const [tags, setTags] = useState({
    json: null,
    name: '',
  });
  const [isSettingsExpanded, setIsSettingsExpanded] = useState(true);
  const [isNodesExpanded, setIsNodesExpanded] = useState(false);
  const [isMetricsExpanded, setIsMetricsExpanded] = useState(false);

  if (!element) return null;
  const { name: cellName, displayName } = element;
  let settings;
  switch (element.kind) {
    case KIND.source:
    case KIND.sink:
      settings = currentConnector.find(
        connector => connector.name === cellName,
      );
      break;
    case KIND.stream:
      settings = currentStream.find(stream => stream.name === cellName);
      break;
    case KIND.topic:
      settings = currentTopic.find(topic => topic.name === cellName);
      break;
    default:
      break;
  }
  const pipelineOnlyTopics = currentTopic.filter(topic => !topic.tags.isShared);
  if (!settings) return null;

  const ignoreList = settings.settingDefinitions
    .filter(def => def.internal)
    .map(def => def.key)
    .concat([
      'brokerClusterKey',
      'workerClusterKey',
      'settings',
      'stagingSettings',
      'settingDefinitions',
      'metrics',
      'tasksStatus',
    ]);

  const handleFullButtonClick = (value, name) => {
    setTags({
      name,
      json: value,
    });

    setIsOpen(true);
  };

  const renderSettings = (settings, key) => {
    const { settingDefinitions: defs } = settings;
    const currentSetting = settings[key];

    if (_.isObject(currentSetting) && !_.isArray(currentSetting)) {
      if (key === 'tags') {
        const dots = '...';
        const value = JSON.stringify(currentSetting);
        const displayValue =
          value.length > 2 ? value.substring(0, 30) + dots + '}' : value;
        const isTruncated = displayValue.endsWith(dots + '}');
        const isPipelineTopic =
          settings.classType === 'topic' && _.has(settings, 'tags.isShared');
        const name = isPipelineTopic
          ? settings.tags.displayName
          : settings.name;

        return (
          <PropertyField
            key={key}
            label={propertyUtils.getDisplayName(key, defs)}
            value={displayValue}
            slot={
              isTruncated && (
                <Tooltip className="settings-full-button" title="Full content">
                  <IconButton
                    size="small"
                    onClick={() => handleFullButtonClick(value, name)}
                  >
                    <CodeIcon fontSize="inherit" />
                  </IconButton>
                </Tooltip>
              )
            }
          />
        );
      } else {
        return (
          <div className="field-wrapper" key={key}>
            <TreeView
              defaultCollapseIcon={<ArrowRightIcon />}
              defaultExpandIcon={<ArrowDropUpIcon />}
            >
              <TreeItem
                nodeId={key}
                label={propertyUtils.getDisplayName(key, defs)}
              >
                {Object.keys(currentSetting).map(objectKey => {
                  return (
                    <TreeItem
                      key={objectKey}
                      nodeId={objectKey}
                      label={`${objectKey} : ${settings[objectKey]}`}
                    />
                  );
                })}
              </TreeItem>
            </TreeView>
          </div>
        );
      }
    } else if (_.isArray(currentSetting)) {
      const isPrimitive = currentSetting.every(setting => !_.isObject(setting));
      if (isPrimitive) {
        // need to do a conversion here, the value could be number
        const value = String(currentSetting);
        const displayValue = value ? value : '';
        return (
          <PropertyField
            key={key}
            label={propertyUtils.getDisplayName(key, defs)}
            value={displayValue}
          />
        );
      } else {
        const checkValue = value => {
          const pipelineOnlyTopic = pipelineOnlyTopics.find(
            topic => topic.name === value,
          );
          return pipelineOnlyTopic ? pipelineOnlyTopic.tags.displayName : value;
        };
        return (
          <div className="field-wrapper" key={key}>
            <TreeView
              defaultCollapseIcon={<ArrowRightIcon />}
              defaultExpandIcon={<ArrowDropUpIcon />}
            >
              <TreeItem
                nodeId={key}
                label={`${propertyUtils.getDisplayName(key, defs)}`}
              >
                {currentSetting.map((item, index) => {
                  return (
                    <TreeItem key={index} nodeId={String(index)} label="Object">
                      {Object.keys(item)
                        .filter(key => key !== 'group')
                        .map(objectKey => {
                          return (
                            <TreeItem
                              nodeId={checkValue(item[objectKey])}
                              key={objectKey}
                              label={`${objectKey} : ${checkValue(
                                item[objectKey],
                              )}`}
                            />
                          );
                        })}
                    </TreeItem>
                  );
                })}
              </TreeItem>
            </TreeView>
          </div>
        );
      }
    } else {
      // Rendering common field
      // need to do a conversion here, the value could be number
      const value = String(currentSetting);
      const { documentation } = settings.settingDefinitions.find(
        def => def.key === key,
      );
      const displayValue = value ? value : '';

      // If the value is truncated, then users won't able to see the whole value
      // therefore, we're using a tooltip here to display the full value
      return (
        <PropertyField
          key={key}
          label={propertyUtils.getDisplayName(key, defs)}
          value={displayValue}
          documentation={documentation}
        />
      );
    }
  };
  const { tasksStatus = [] } = settings;
  const hasNodesInfo = tasksStatus.length > 0;
  const hasMetrics = metrics.meters.length > 0;
  return (
    <Wrapper variant="outlined" square>
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
                variant="body2"
                component="span"
                color="textSecondary"
              >
                Status:
              </Typography>
              <Typography
                className="status-value"
                variant="body2"
                component="span"
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

      <ExpansionPanel
        square
        defaultExpanded={true}
        expanded={isSettingsExpanded}
      >
        <ExpansionPanelSummary
          onClick={() => setIsSettingsExpanded(prevState => !prevState)}
          expandIcon={<ExpandMoreIcon />}
        >
          <TuneIcon fontSize="small" />
          <Typography className="section-title" variant="h5">
            Settings
          </Typography>
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          {Object.keys(settings)
            .filter(key => !ignoreList.includes(key))
            .filter(key => {
              // We're not displaying empty array or object
              const item = settings[key];
              if (_.isObject(item) && _.isEmpty(item)) return false;

              return true;
            })
            .filter(key => {
              return settings.settingDefinitions
                .map(def => def.key)
                .find(defKey => defKey === key);
            })
            .map(key => renderSettings(settings, key))}
        </ExpansionPanelDetails>
      </ExpansionPanel>

      {hasNodesInfo && (
        <ExpansionPanel defaultExpanded={true} expanded={isNodesExpanded}>
          <ExpansionPanelSummary
            onClick={() => setIsNodesExpanded(prevState => !prevState)}
            expandIcon={<ExpandMoreIcon />}
          >
            <StorageIcon fontSize="small" />
            <Typography className="section-title" variant="h5">
              Nodes
            </Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails>
            {tasksStatus.map(node => {
              const { nodeName, state } = node;
              return (
                <PropertyField
                  key={nodeName}
                  label="Name"
                  value={nodeName}
                  slot={
                    <Typography
                      variant="body2"
                      className="node-status"
                      component="span"
                    >
                      {state}
                    </Typography>
                  }
                />
              );
            })}
          </ExpansionPanelDetails>
        </ExpansionPanel>
      )}

      {hasMetrics && (
        <ExpansionPanel defaultExpanded={true} expanded={isMetricsExpanded}>
          <ExpansionPanelSummary
            onClick={() => setIsMetricsExpanded(prevState => !prevState)}
            expandIcon={<ExpandMoreIcon />}
          >
            <SignalCellularAltIcon fontSize="small" />
            <Typography className="section-title" variant="h5">
              Metrics
            </Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails>
            {metrics.meters.map(metric => {
              const { document, value, unit } = metric;
              return (
                <PropertyField
                  key={document}
                  label={document}
                  value={value}
                  slot={
                    <Typography
                      variant="body2"
                      className="metrics-unit"
                      component="span"
                    >
                      {unit}
                    </Typography>
                  }
                />
              );
            })}
          </ExpansionPanelDetails>
        </ExpansionPanel>
      )}

      <Dialog
        handleClose={() => setIsOpen(false)}
        open={isOpen}
        title={`Full tags content of ${tags.name}`}
        showActions={false}
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
  element: PropTypes.object,
  cellsMetrics: PropTypes.array,
};

export default PipelinePropertyView;
