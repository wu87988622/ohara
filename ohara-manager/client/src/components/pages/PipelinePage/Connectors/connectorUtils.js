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
import { Field } from 'react-final-form';

import ColumnTable from './CustomConnector/ColumnTable';
import { FormGroup, Label } from 'common/Form';
import { InputField, SelectField } from 'common/FormFields';
import { CONNECTOR_STATES } from 'constants/pipelines';
import { isEmptyStr } from 'utils/commonUtils';
import { findByGraphId } from '../pipelineUtils/commonUtils';

export const updateConfigs = ({ configs, target }) => {
  const { value, name } = target;
  const update = {
    ...configs,
    [name]: value,
  };
  return update;
};

export const getCurrTopicId = ({ originals, target = '' }) => {
  if (isEmpty(originals) || isEmptyStr(target)) return;
  const findByTopicName = ({ name }) => name === target;
  const { id } = originals.find(findByTopicName);
  return id;
};

export const getCurrTopicName = ({ originals, target }) => {
  const topicId = get(target, '[0]', '');
  const findByTopicId = ({ id }) => id === topicId;
  const currTopic = originals.find(findByTopicId);
  const topicName = get(currTopic, 'name', '');
  return topicName;
};

export const getUpdatedTopic = ({
  graph,
  configs,
  connectorId,
  currTopicId,
  originalTopics,
}) => {
  const connector = findByGraphId(graph, connectorId);
  const connectorName = configs['connector.name'];
  let update;

  if (connector.kind === 'source') {
    const findByTopicName = topic => topic.name === configs.topics;
    const currTopic = originalTopics.find(findByTopicName);
    const topicId = isEmpty(configs.topics) ? [] : [currTopic.id];
    update = { ...connector, name: connectorName, to: topicId };
  } else {
    const currSink = findByGraphId(graph, connectorId);
    const findByCurrTopicId = g => g.id === currTopicId;
    const topic = graph.find(findByCurrTopicId);

    // Extra props for sink connector to properly render
    const sinkProps = {
      isFromTopic: true,
      updatedName: connectorName,
      sinkId: connectorId,
    };

    if (topic) {
      const to = [...new Set([...topic.to, connectorId])];
      update = { sinkProps, update: { ...topic, to } };
    } else {
      update = { sinkProps, update: { ...currSink } };
    }
  }

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

export const deleteColumnRow = ({ configs, currRow }) => {
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

export const switchType = type => {
  switch (type) {
    case 'STRING':
      return 'text';
    case 'INT':
      return 'number';
    default:
      return 'text';
  }
};

export const convertData = ({ configValue, valueType, defaultValue }) => {
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

export const changeToken = ({ values, targetToken, replaceToken }) => {
  return Object.keys(values).reduce((acc, key) => {
    // Two tokens we use to separate words: `.` and `_`
    const pattern = targetToken === '.' ? /\./g : /_/g;
    const renamedObject = {
      [key.replace(pattern, replaceToken)]: values[key],
    };

    return {
      ...acc,
      ...renamedObject,
    };
  }, {});
};

export const getRenderData = ({ state, defs, configs }) => {
  const isRunning =
    state === CONNECTOR_STATES.running || state === CONNECTOR_STATES.failed;
  const sortByOrder = (a, b) => a.orderInGroup - b.orderInGroup;

  const data = defs
    .sort(sortByOrder)
    .filter(def => !def.internal) // Don't display these defs
    .map(def => {
      const { key, defaultValue, valueType } = def;

      let _key;
      let arr;
      if (key.includes('.')) {
        arr = key.split('.');
        _key = arr.join('_');
      } else {
        arr = key.split('_');
        _key = arr.join('.');
      }

      const configValue = configs[_key];
      const displayValue = convertData({
        configValue,
        valueType,
        defaultValue,
      });

      return {
        ...def,
        configValue,
        displayValue,
        isRunning,
        key: _key,
      };
    });

  return data;
};

export const renderForm = ({
  data,
  topics,
  handleChange,
  handleColumnChange,
  handleColumnRowDelete,
  handleColumnRowUp,
  handleColumnRowDown,
}) => {
  const dataType = ['STRING'];
  const tableActions = ['Up', 'Down', 'Delete'];

  return data.map(d => {
    const {
      valueType,
      key,
      displayName,
      required,
      documentation,
      editable,
      isRunning,
      displayValue,
      tableKeys,
    } = d;

    const columnTableHeader = tableKeys.concat(tableActions);

    if (['STRING', 'INT', 'CLASS'].includes(valueType)) {
      const inputType = switchType(valueType);

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
            type={inputType}
            component={InputField}
            id={`${displayName}`}
            width="100%"
            parse={value => String(value)}
            name={key}
            onChange={handleChange}
            disabled={!editable || isRunning}
          />
        </FormGroup>
      );
    } else if (valueType === 'LIST') {
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
            component={SelectField}
            name={key}
            width="100%"
            disabled={isRunning}
            clearable
          />
        </FormGroup>
      );
    } else if (valueType === 'TABLE') {
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
    }

    return null;
  });
};
