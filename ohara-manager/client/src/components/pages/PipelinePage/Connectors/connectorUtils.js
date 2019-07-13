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
import { get, isEmpty, isUndefined, isNull } from 'lodash';
import { Field } from 'react-final-form';

import ColumnTable from './CustomConnector/ColumnTable';
import Tabs from './Tabs';
import { FormGroup } from 'components/common/Form';
import { InputField, Select, Checkbox } from 'components/common/Mui/Form';
import { findByGraphName } from '../pipelineUtils/commonUtils';

export const getCurrTopicId = ({ originals, target = '' }) => {
  if (isEmpty(originals) || isNull(target) || target === 'Please select...')
    return [];

  const findByTopicName = ({ name }) => name === target;
  const { name } = originals.find(findByTopicName);

  return name;
};

export const getCurrTopicName = ({ originals, target }) => {
  const topicName = get(target, '[0]', '');
  return topicName;
};

export const getUpdatedTopic = ({
  graph,
  configs,
  connectorName,
  currTopicName,
  originalTopics,
}) => {
  const connector = findByGraphName(graph, connectorName);
  let update;

  if (connector.kind === 'source') {
    const findByTopicName = topic => topic.name === configs.topics;
    const currTopic = originalTopics.find(findByTopicName);
    const topicName = isUndefined(currTopic) ? [] : [currTopic.name];
    update = { update: { ...connector, name: connectorName, to: topicName } };
  } else {
    const currSink = findByGraphName(graph, connectorName);
    const findByCurrTopicName = g => g.name === currTopicName;
    const topic = graph.find(findByCurrTopicName);

    // Extra props for sink connector to properly render
    const sinkProps = {
      isFromTopic: true,
      updatedName: connectorName,
      sinkName: connectorName,
    };

    if (topic) {
      const to = [...new Set([...topic.to, connectorName])];
      update = { sinkProps, update: { ...topic, to } };
    } else {
      update = { sinkProps, update: { ...currSink } };
    }
  }
  return update;
};

export const addColumn = ({ configs, update }) => {
  const { columns = [] } = configs;
  const {
    columnName: name,
    newColumnName: newName,
    currType: dataType,
    parentValues,
  } = update;

  let order = 0;
  if (isEmpty(columns)) {
    order = 1;
  } else {
    order = columns[columns.length - 1].order + 1;
  }

  const newColumn = {
    order,
    name,
    newName,
    dataType,
  };

  const updatedConfigs = {
    ...parentValues,
    columns: [...columns, newColumn],
  };

  return updatedConfigs;
};

export const deleteColumnRow = ({ configs, update }) => {
  const { parentValues, currRow } = update;

  const { columns } = configs;
  const updatedColumns = columns
    .filter(column => column.order !== currRow)
    .map((column, index) => ({ ...column, order: ++index }));

  const updatedConfigs = { ...parentValues, columns: [...updatedColumns] };
  return updatedConfigs;
};

export const moveColumnRowUp = ({ configs, update }) => {
  const { columns } = configs;
  const { order, parentValues } = update;

  if (order === 1) return;

  const index = columns.findIndex(s => s.order === order);

  const updatedColumns = [
    ...columns.slice(0, index - 1),
    columns[index],
    columns[index - 1],
    ...columns.slice(index + 1),
  ].map((columns, index) => ({ ...columns, order: ++index }));
  const updatedConfigs = { ...parentValues, columns: [...updatedColumns] };

  return updatedConfigs;
};

export const moveColumnRowDown = ({ configs, update }) => {
  const { columns } = configs;
  const { order, parentValues } = update;

  if (order === columns.length) return;

  const index = columns.findIndex(s => s.order === order);

  const updatedColumns = [
    ...columns.slice(0, index),
    columns[index + 1],
    columns[index],
    ...columns.slice(index + 2),
  ].map((columns, index) => ({ ...columns, order: ++index }));
  const updatedConfigs = { ...parentValues, columns: [...updatedColumns] };

  return updatedConfigs;
};

export const switchType = type => {
  switch (type) {
    case 'STRING':
      return 'text';
    case 'INT':
    case 'PORT':
    case 'LONG':
      return 'number';
    case 'PASSWORD':
      return 'password';
    default:
      // Don't render the field since we can't recognize the type
      return null;
  }
};

export const getEditable = ({ key, defaultEditable }) => {
  // we're overriding these two editable values here as
  // - name: is a value that can only be filled once and can't be changed later on
  // - connector_class: cannot be edited in the UI, or it will simply break
  return key === 'name' || key === 'connector_class' ? false : defaultEditable;
};

export const getDisplayValue = ({ configValue, defaultValue }) => {
  return configValue ? configValue : defaultValue;
};

export const changeToken = ({ values, targetToken, replaceToken }) => {
  return Object.keys(values).reduce((acc, key) => {
    // Two tokens we use to separate words: `.` and `_`
    // we are doing this here because final form treats `foo.bar`
    // as nested object and expands `foo.bar` to `{ foo: { bar } }`
    // and due to the API restriction, we will need to work on this
    // in the frontend 😅
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

export const changeKeySeparator = key => {
  let result;
  let arr;
  if (key.includes('.')) {
    arr = key.split('.');
    result = arr.join('_');
  } else {
    arr = key.split('_');
    result = arr.join('.');
  }

  return result;
};

export const sortByOrder = (a, b) => a.orderInGroup - b.orderInGroup;

export const getRenderData = ({ state, defs, configs }) => {
  const isRunning = !!state; // Any state indicates the connector is running

  const data = defs
    .sort(sortByOrder)
    .filter(def => !def.internal) // internal defs are not meant to be seen by users
    .map(def => {
      const { key, defaultValue } = def;
      const newKey = changeKeySeparator(key);
      const configValue = configs[newKey];

      const displayValue = getDisplayValue({
        configValue,
        defaultValue,
      });

      const editable = getEditable({
        key: newKey,
        defaultEditable: def.editable,
      });

      return {
        ...def,
        displayValue,
        editable,
        isRunning,
        key: newKey,
      };
    });

  return data;
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

export const renderer = props => {
  const {
    formData,
    topics,
    handleColumnChange,
    handleColumnRowDelete,
    handleColumnRowUp,
    handleColumnRowDown,
    parentValues,
  } = props;

  const dataType = ['String'];
  const tableActions = ['Up', 'Down', 'Delete'];

  const renderWithValueType = params => {
    const {
      valueType,
      key,
      displayName,
      documentation,
      editable,
      isRunning,
      tableKeys,
      displayValue,
    } = params;
    const columnTableHeader = tableKeys.concat(tableActions);
    switch (valueType) {
      case 'STRING':
      case 'INT':
      case 'CLASS':
      case 'PASSWORD':
      case 'JDBC_TABLE':
      case 'LONG':
      case 'PORT':
        const inputType = switchType(valueType);

        return (
          <FormGroup key={key}>
            <Field
              type={inputType}
              component={InputField}
              label={displayName}
              id={displayName}
              helperText={documentation}
              width="100%"
              name={key}
              disabled={!editable || isRunning}
            />
          </FormGroup>
        );

      case 'BOOLEAN':
        return (
          <FormGroup key={key}>
            <Field
              label={displayName}
              name={key}
              type="checkbox"
              helperText={documentation}
              component={Checkbox}
              disabled={!editable || isRunning}
            />
          </FormGroup>
        );

      case 'TABLE':
        return (
          <FormGroup key={key}>
            <ColumnTable
              parentValues={parentValues}
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
  };

  const renderWithReference = params => {
    const { reference, key, displayName, isRunning, documentation } = params;
    switch (reference) {
      case 'TOPIC':
        return (
          <FormGroup key={key}>
            <Field
              label={displayName}
              id={displayName}
              list={topics}
              component={Select}
              name={key}
              width="100%"
              disabled={isRunning}
            />
          </FormGroup>
        );
      case 'WORKER_CLUSTER':
        return (
          <FormGroup key={key}>
            <Field
              type="string"
              component={InputField}
              label={displayName}
              id={displayName}
              helperText={documentation}
              width="100%"
              name={key}
              disabled={true}
            />
          </FormGroup>
        );
      default:
        return;
    }
  };

  return formData.map(data => {
    const { reference } = data;
    if (reference === 'NONE') {
      return renderWithValueType(data);
    } else {
      return renderWithReference(data);
    }
  });
};

export const renderForm = props => {
  const groupedDefs = groupBy(props.formData, item => {
    return [item.group];
  });

  const hasTab = groupedDefs.length > 1;

  if (hasTab) {
    return <Tabs {...props} groupedDefs={groupedDefs} renderer={renderer} />;
  } else {
    return groupedDefs.sort().map(def => {
      const renderData = { def, ...props };
      return renderer(renderData);
    });
  }
};
