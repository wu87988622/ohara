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
import WavesIcon from '@material-ui/icons/Waves';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import StorageIcon from '@material-ui/icons/Storage';
import TreeView from '@material-ui/lab/TreeView';
import TreeItem from '@material-ui/lab/TreeItem';
import ArrowRightIcon from '@material-ui/icons/ArrowRight';
import ArrowDropUpIcon from '@material-ui/icons/ArrowDropUp';

import CodeIcon from '@material-ui/icons/Code';
import { AddSharedTopicIcon } from 'components/common/Icon';
import { Tooltip } from 'components/common/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import PropertyField from './PipelinePropertyViewField';
import { KIND } from 'const';

export const renderIcon = ({ kind, isShared }) => {
  switch (kind) {
    case KIND.source:
      return <FlightTakeoffIcon color="action" />;
    case KIND.sink:
      return <FlightLandIcon color="action" />;
    case KIND.stream:
      return <WavesIcon color="action" />;
    case KIND.topic:
      if (isShared) {
        return <AddSharedTopicIcon height={20} width={20} />;
      } else {
        return <StorageIcon color="action" />;
      }
    default:
      break;
  }
};

export const getDisplayName = (key, defs) => {
  const setting = defs.find(def => def.key === key);
  return setting ? setting.displayName : key;
};

export const tags = params => {
  const {
    currentSetting,
    settings,
    key,
    defs,
    propertyUtils,
    handleFullButtonClick,
  } = params;
  const dots = '...';
  const value = JSON.stringify(currentSetting);
  const displayValue =
    value.length > 2 ? value.substring(0, 30) + dots + '}' : value;
  const isTruncated = displayValue.endsWith(dots + '}');
  const isPipelineTopic = settings.classType === 'topic' && !settings.isShared;
  const name = isPipelineTopic ? settings.displayName : settings.name;

  return (
    <PropertyField
      key={key}
      label={propertyUtils.getDisplayName(key, defs)}
      slot={
        isTruncated && (
          <Tooltip className="settings-full-button" title="Full content">
            <IconButton
              onClick={() => handleFullButtonClick(value, name)}
              size="small"
            >
              <CodeIcon fontSize="inherit" />
            </IconButton>
          </Tooltip>
        )
      }
      value={displayValue}
    />
  );
};

export const objectField = (key, settings, currentSetting, defs) => {
  return (
    <div className="field-wrapper" key={key}>
      <TreeView
        defaultCollapseIcon={<ArrowRightIcon />}
        defaultExpandIcon={<ArrowDropUpIcon />}
      >
        <TreeItem label={getDisplayName(key, defs)} nodeId={key}>
          {Object.keys(currentSetting).map(objectKey => {
            return (
              <TreeItem
                key={objectKey}
                label={`${objectKey} : ${settings[objectKey]}`}
                nodeId={objectKey}
              />
            );
          })}
        </TreeItem>
      </TreeView>
    </div>
  );
};

export const objectKeys = (key, defs, currentSetting) => {
  return (
    <div className="field-wrapper" key={key}>
      <TreeView
        defaultCollapseIcon={<ArrowRightIcon />}
        defaultExpandIcon={<ArrowDropUpIcon />}
      >
        <TreeItem label={`${getDisplayName(key, defs)}`} nodeId={key}>
          {currentSetting.map((item, index) => {
            return (
              <TreeItem key={index} label="column" nodeId={String(index)}>
                {Object.keys(item).map(objectKey => {
                  return (
                    <TreeItem
                      key={objectKey}
                      label={`${objectKey} : ${item[objectKey]}`}
                      nodeId={item[objectKey]}
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
};

export const defaultField = params => {
  const { currentSetting, settings, key, defs, isPort = false } = params;

  // need to do a conversion here, the value could be number
  const value = String(currentSetting);
  const { documentation = '' } = settings.settingDefinitions.find(
    def => def.key === key,
  );
  const displayValue = value ? value : '';

  // If the value is truncated, then users won't able to see the whole value
  // therefore, we're using a tooltip here to display the full value
  return (
    <PropertyField
      documentation={documentation}
      isPort={isPort}
      key={key}
      label={getDisplayName(key, defs)}
      value={displayValue}
    />
  );
};
