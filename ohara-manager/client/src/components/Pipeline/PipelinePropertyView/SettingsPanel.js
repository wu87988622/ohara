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
import _ from 'lodash';
import TuneIcon from '@material-ui/icons/Tune';
import Typography from '@material-ui/core/Typography';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';

import * as defUtils from 'api/apiInterface/definitionInterface';
import * as propertyUtils from './PipelinePropertyViewUtils';
import * as hooks from 'hooks';

const SettingsPanel = props => {
  const { settings, setTags, setFullTagViewDialogOpen } = props;
  const topics = hooks.useTopicsInPipeline();
  const [isSettingsExpanded, setIsSettingsExpanded] = React.useState(true);

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

  const renderSettings = (settings, key) => {
    const { settingDefinitions: defs } = settings;
    const currentSetting =
      key === 'name'
        ? settings.tags?.displayName
          ? settings.tags.displayName
          : settings[key]
        : settings[key];
    const defValueType = defUtils.Type;
    const valueType = defs.find(def => def.key === key).valueType;
    switch (valueType) {
      case defValueType.TAGS:
        return propertyUtils.tags({
          currentSetting,
          settings,
          key,
          defs,
          propertyUtils,
          handleFullButtonClick,
        });
      case defValueType.REMOTE_PORT:
      case defValueType.BINDING_PORT:
        return propertyUtils.defaultField({
          currentSetting,
          settings,
          key,
          defs,
          isPort: true,
        });
      case defValueType.OBJECT_KEYS:
        const objectArray = currentSetting
          .map(value => value.name)
          .map(value => {
            if (topics.map(topic => topic.name).includes(value)) {
              const topic = topics.find(topic => topic.name === value);
              return topic.tags?.displayName
                ? topic.tags?.displayName
                : topic.name;
            } else {
              return value;
            }
          });
        return propertyUtils.defaultField({
          currentSetting: objectArray,
          settings,
          key,
          defs,
        });
      case defValueType.TABLE:
        return propertyUtils.objectKeys(key, defs, currentSetting);
      case defValueType.PASSWORD:
        // Don't display the real password
        return propertyUtils.defaultField({
          currentSetting: '*'.repeat(9),
          settings,
          key,
          defs,
        });
      default:
        return propertyUtils.defaultField({
          currentSetting,
          settings,
          key,
          defs,
        });
    }
  };

  const handleFullButtonClick = (value, name) => {
    setTags({
      name,
      json: value,
    });

    setFullTagViewDialogOpen(true);
  };

  return (
    <ExpansionPanel square defaultExpanded={true} expanded={isSettingsExpanded}>
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
  );
};

SettingsPanel.propTypes = {
  settings: PropTypes.shape({
    settingDefinitions: PropTypes.arrayOf(PropTypes.object).isRequired,
    tags: PropTypes.shape({
      displayName: PropTypes.string,
    }).isRequired,
  }).isRequired,
  setTags: PropTypes.func.isRequired,
  setFullTagViewDialogOpen: PropTypes.func.isRequired,
};

export default SettingsPanel;
