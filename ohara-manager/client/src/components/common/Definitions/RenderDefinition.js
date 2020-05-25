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
import { Field } from 'react-final-form';
import { toNumber, isEmpty, includes } from 'lodash';

import BindingPort from './BindingPort';
import BooleanDef from './BooleanDef';
import Chooser from './Chooser';
import Duration from './Duration';
import IntDef from './IntDef';
import JdbcTable from './JdbcTable';
import Password from './Password';
import RemotePort from './RemotePort';
import Reference from './Reference';
import StringDef from './StringDef';
import Table from './Table';
import Tags from './Tags';
import PositiveInt from './PositiveInt';
import ClassDef from './ClassDef';
import Long from './Long';
import Short from './Short';
import Double from './Double';
import ArrayDef from './ArrayDef';
import ObjectKey from './ObjectKey';
import ObjectKeys from './ObjectKeys';
import PositiveDouble from './PositiveDouble';
import PositiveLong from './PositiveLong';
import PositiveShort from './PositiveShort';
import { validWithDef } from 'utils/validate';
import { CREATE_ONLY, EDITABLE } from './Permission';
import {
  Type,
  Reference as ReferenceEnum,
  isNumberType,
} from 'api/apiInterface/definitionInterface';

const RenderDefinition = props => {
  const {
    def,
    topics = [],
    files,
    nodes = [],
    defType,
    ref,
    freePorts,
  } = props;

  const parseValueByType = type => value => {
    // we only convert the necessary values to correct type
    return isNumberType(type) ? toNumber(value) : value;
  };

  const getFieldProps = definition => {
    const {
      key,
      displayName,
      documentation,
      necessary,
      permission,
      valueType,
    } = definition;

    let disabled = false;
    switch (permission) {
      case 'READ_ONLY':
        disabled = true;
        break;

      case 'CREATE_ONLY':
        disabled = defType !== CREATE_ONLY;
        break;

      case 'EDITABLE':
        disabled = defType !== EDITABLE;
        break;

      default:
        break;
    }

    const ensuredFieldProps = {
      key,
      name: key,
      label: displayName,
      helperText: documentation,
      disabled,
      required: necessary === 'REQUIRED',
      validate: validWithDef(definition),
      parse: parseValueByType(valueType),
    };

    if (ref) {
      ensuredFieldProps.refs = ref;
    }

    return ensuredFieldProps;
  };

  const renderDefinitionField = () => {
    const { valueType, reference, tableKeys } = def;
    const ensuredFieldProps = getFieldProps(def);

    // Only the following types have support reference:
    // 1. ARRAY
    // 2. OBJECT_KEYS
    // 3. STRING
    const valueTypesForSupport = [Type.ARRAY, Type.OBJECT_KEYS, Type.STRING];
    const referencesForSupport = [
      ReferenceEnum.TOPIC,
      ReferenceEnum.FILE,
      ReferenceEnum.NODE,
    ];

    if (
      includes(referencesForSupport, reference) &&
      includes(valueTypesForSupport, valueType)
    ) {
      const multiple =
        valueType === Type.ARRAY || valueType === Type.OBJECT_KEYS;
      switch (reference) {
        case ReferenceEnum.TOPIC:
          return (
            <Field {...ensuredFieldProps} component={Reference} list={topics} />
          );
        case ReferenceEnum.FILE:
          return (
            <Field {...ensuredFieldProps} component={Reference} list={files} />
          );
        case ReferenceEnum.NODE:
          return (
            <Field
              {...ensuredFieldProps}
              component={Chooser}
              multipleChoice={multiple}
              options={nodes.map(node => node.hostname)}
            />
          );
        default:
          throw new Error(`Unsupported reference: ${reference}`);
      }
    } else {
      switch (valueType) {
        case Type.STRING:
          return <Field {...ensuredFieldProps} component={StringDef} />;

        case Type.REMOTE_PORT:
          return <Field {...ensuredFieldProps} component={RemotePort} />;

        case Type.INT:
          return <Field {...ensuredFieldProps} component={IntDef} />;

        case Type.CLASS:
          return <Field {...ensuredFieldProps} component={ClassDef} />;

        case Type.PASSWORD:
          return <Field {...ensuredFieldProps} component={Password} />;

        case Type.POSITIVE_INT:
          return <Field {...ensuredFieldProps} component={PositiveInt} />;

        case Type.DURATION:
          return <Field {...ensuredFieldProps} component={Duration} />;

        case Type.BINDING_PORT:
          if (isEmpty(freePorts)) {
            return <Field {...ensuredFieldProps} component={RemotePort} />;
          } else {
            return (
              <Field
                {...ensuredFieldProps}
                component={BindingPort}
                list={freePorts}
              />
            );
          }

        case Type.TAGS:
          return <Field {...ensuredFieldProps} component={Tags} />;

        case Type.JDBC_TABLE:
          return <Field {...ensuredFieldProps} component={JdbcTable} />;

        case Type.TABLE:
          return (
            <Field
              {...ensuredFieldProps}
              children={Table}
              tableKeys={tableKeys}
            />
          );

        case Type.BOOLEAN:
          return (
            <Field
              {...ensuredFieldProps}
              component={BooleanDef}
              type={valueType === Type.BOOLEAN ? 'checkbox' : null}
            />
          );

        case Type.LONG:
          return <Field {...ensuredFieldProps} component={Long} />;

        case Type.SHORT:
          return <Field {...ensuredFieldProps} component={Short} />;

        case Type.DOUBLE:
          return <Field {...ensuredFieldProps} component={Double} />;

        case Type.ARRAY:
          return <Field {...ensuredFieldProps} component={ArrayDef} />;

        case Type.POSITIVE_SHORT:
          return <Field {...ensuredFieldProps} component={PositiveShort} />;

        case Type.POSITIVE_LONG:
          return <Field {...ensuredFieldProps} component={PositiveLong} />;

        case Type.POSITIVE_DOUBLE:
          return <Field {...ensuredFieldProps} component={PositiveDouble} />;

        case Type.OBJECT_KEYS:
          return <Field {...ensuredFieldProps} component={ObjectKeys} />;

        case Type.OBJECT_KEY:
          return <Field {...ensuredFieldProps} component={ObjectKey} />;

        default:
          return <Field {...ensuredFieldProps} component={StringDef} />;
      }
    }
  };

  return renderDefinitionField();
};

export default RenderDefinition;
