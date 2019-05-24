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
import { get, isEmpty } from 'lodash';

import ColumnTable from './ColumnTable';
import { isEmptyStr } from 'utils/commonUtils';
import { findByGraphId } from '../../pipelineUtils/commonUtils';
import { FormGroup, Label } from 'common/Form';
import { InputField, SelectField } from 'common/FormFields';
import { CONNECTOR_STATES } from 'constants/pipelines';
import UtilsTabs from './UtilsTabs';
import { Field } from 'react-final-form';

export const getMetadata = (props, worker) => {
  const { page: targetConnector } = props.match.params;
  const { connectors } = worker;

  const { className: connectorName, definitions: defs } = connectors.find(
    c => c.className === targetConnector,
  );

  const configs = defs.reduce((acc, config) => {
    const { key, defaultValue } = config;

    acc[key] = defaultValue;
    return acc;
  }, {});

  return { connectorName, defs, configs };
};

export const updateConfigs = ({ configs, target }) => {
  const { value, name } = target;

  const update = {
    ...configs,
    [name]: value,
  };

  return update;
};

export const addColumn = ({ configs, newColumn }) => {
  const { columns = [] } = configs;
  const {
    columnName: name,
    newColumnName: newName,
    currType: dataType,
  } = newColumn;

  let order = 0;
  if (isEmpty(columns)) {
    order = 1;
  } else {
    order = columns[columns.length - 1].order + 1;
  }

  const update = {
    order,
    name,
    newName,
    dataType,
  };

  const updatedConfigs = {
    ...configs,
    columns: [...columns, update],
  };

  return updatedConfigs;
};

export const removeColumn = ({ configs, currRow }) => {
  const { columns } = configs;
  const updatedColumns = columns
    .filter(column => column.order !== currRow)
    .map((column, idx) => ({ ...column, order: ++idx }));

  const updatedConfigs = { ...configs, columns: [...updatedColumns] };
  return updatedConfigs;
};

export const moveColumnRowUp = ({ configs, order }) => {
  const { columns } = configs;

  if (order === 1) return;

  const idx = columns.findIndex(s => s.order === order);

  const updatedColumns = [
    ...columns.slice(0, idx - 1),
    columns[idx],
    columns[idx - 1],
    ...columns.slice(idx + 1),
  ].map((columns, idx) => ({ ...columns, order: ++idx }));
  const updatedConfigs = { ...configs, columns: [...updatedColumns] };

  return updatedConfigs;
};

export const moveColumnRowDown = ({ configs, order }) => {
  const { columns } = configs;

  if (order === columns.length) return;

  const idx = columns.findIndex(s => s.order === order);

  const updatedColumns = [
    ...columns.slice(0, idx),
    columns[idx + 1],
    columns[idx],
    ...columns.slice(idx + 2),
  ].map((columns, idx) => ({ ...columns, order: ++idx }));
  const updatedConfigs = { ...configs, columns: [...updatedColumns] };

  return updatedConfigs;
};

export const getUpdatedTopic = ({
  graph,
  configs,
  connectorId,
  originalTopics,
}) => {
  const connector = findByGraphId(graph, connectorId);
  const currTopic = originalTopics.find(topic => topic.name === configs.topics);
  const topicId = isEmpty(configs.topics) ? [] : [currTopic.id];
  const name = configs['connector.name'];
  const update = { ...connector, name, to: topicId };
  return update;
};

export const typeSwitch = type => {
  switch (type) {
    case 'PASSWORD':
      return 'password';
    default:
      return null;
  }
};

export const groupBy = (array, fn) => {
  let groups = {};
  array.forEach(o => {
    let group = JSON.stringify(fn(o));
    groups[group] = groups[group] || [];
    groups[group].push(o);
  });
  return Object.keys(groups).map(group => {
    return groups[group];
  });
};

export const replaceKeys = obj => {
  if (obj == null) {
    return null;
  }
  return Object.keys(obj).reduce((acc, key) => {
    const renamedObject = {
      [key.replace(/\./g, '_')]: obj[key],
    };

    return {
      ...acc,
      ...renamedObject,
    };
  }, {});
};

export const renderForm = ({
  state,
  defs,
  configs,
  topics,
  handleChange,
  handleColumnChange,
  handleColumnRowDelete,
  handleColumnRowUp,
  handleColumnRowDown,
}) => {
  const isRunning =
    state === CONNECTOR_STATES.running || state === CONNECTOR_STATES.failed;

  const dataType = ['STRING'];
  const tableActions = ['Up', 'Down', 'Delete'];
  const sortByOrder = (a, b) => a.orderInGroup - b.orderInGroup;
  const convertData = ({ configValue, valueType, defaultValue }) => {
    let displayValue;
    if (!configValue) {
      // react complains null values
      if (valueType === 'TABLE') {
        displayValue = [];
      } else {
        displayValue = defaultValue || '';
      }
    } else {
      // If we have values returned from connector API, let's use them
      displayValue = configValue;
    }

    return displayValue;
  };

  const defsToFormGroup = defs => {
    return defs
      .sort(sortByOrder)
      .filter(def => !def.internal) // Do not display def that has an internal === true prop
      .map(def => {
        const {
          displayName,
          key,
          editable,
          required,
          documentation,
          tableKeys,
          defaultValue,
          valueType,
        } = def;
        const configValue = configs[key];
        const columnTableHeader = tableKeys.concat(tableActions);
        const replaceKey = key.replace(/\./g, '_');
        const displayValue = convertData({
          configValue,
          valueType,
          defaultValue,
        });
        switch (valueType) {
          case 'STRING':
          case 'INT':
          case 'CLASS':
          case 'PASSWORD':
            const type = typeSwitch(valueType);
            return (
              <FormGroup key={key}>
                <Label
                  htmlFor={`${displayName}`}
                  required={required}
                  tooltipString={documentation}
                  tooltipAlignment="right"
                  width="100%"
                >
                  {displayName}
                </Label>
                <Field
                  id={`${displayName}`}
                  width="100%"
                  name={replaceKey}
                  type={type}
                  onChange={handleChange}
                  disabled={!editable || isRunning}
                  component={InputField}
                />
              </FormGroup>
            );

          case 'LIST':
            return (
              <FormGroup key={key}>
                <Label
                  htmlFor={`${displayName}`}
                  required={required}
                  tooltipString={documentation}
                  tooltipAlignment="right"
                  width="100%"
                >
                  {displayName}
                </Label>
                <Field
                  id={`${displayName}`}
                  list={topics}
                  onChange={handleChange}
                  name={replaceKey}
                  width="100%"
                  disabled={isRunning}
                  clearable
                  component={SelectField}
                />
              </FormGroup>
            );

          case 'TABLE':
            return (
              <FormGroup key={key}>
                <ColumnTable
                  headers={columnTableHeader}
                  data={displayValue}
                  dataTypes={dataType}
                  handleColumnChange={handleColumnChange}
                  handleColumnRowDelete={handleColumnRowDelete}
                  handleColumnRowUp={handleColumnRowUp}
                  handleColumnRowDown={handleColumnRowDown}
                />
              </FormGroup>
            );

          default:
            return null;
        }
      });
  };

  const groupDefs = groupBy(defs, item => {
    return [item.group];
  });
  const hasTab = groupDefs.length > 1 ? true : false;
  if (hasTab) {
    return (
      <form>
        <UtilsTabs groupDefs={groupDefs} defsToFormGroup={defsToFormGroup} />
      </form>
    );
  } else {
    return groupDefs.sort().map(defs => {
      return defsToFormGroup(defs);
    });
  }
};

export const getCurrTopicId = ({ originals, target = '' }) => {
  if (isEmpty(originals) || isEmptyStr(target)) return;

  const topics = [];
  const findByTopicName = ({ name }) => name === target;
  const { id } = originals.find(findByTopicName);
  topics.push(id);

  return topics;
};

export const getCurrTopicName = ({ originals, target }) => {
  const topicId = get(target, '[0]', '');
  const findByTopicId = ({ id }) => id === topicId;
  const currTopic = originals.find(findByTopicId);
  const topicName = get(currTopic, 'name', '');
  return topicName;
};
