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
import { toNumber } from 'lodash';

import BindingPort from './BindingPort';
import BooleanDef from './BooleanDef';
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
import { valueType } from 'api/utils/definitionsUtils';

const RenderDefinition = props => {
  const {
    def,
    topics = [],
    files,
    defType,
    ref,
    fieldProps = {},
    freePorts,
  } = props;

  const finalTopics = topics.map(topic => {
    const { name, tags } = topic;
    const finalTopic = { name: '', tags: { displayName: '' } };
    if (!tags.isShared) {
      finalTopic.name = tags.displayName;
      finalTopic.tags.displayName = name;
      finalTopic.tags.isShared = tags.isShared;
    } else {
      finalTopic.name = name;
      finalTopic.tags.displayName = name;
      finalTopic.tags.isShared = tags.isShared;
    }

    return finalTopic;
  });

  const parseValueByType = type => value => {
    switch (type) {
      // we only convert the necessary values to correct type
      case valueType.short:
      case valueType.int:
      case valueType.long:
      case valueType.double:
      case valueType.positiveShort:
      case valueType.positiveInt:
      case valueType.positiveLong:
      case valueType.positiveDouble:
      case valueType.remotePort:
      case valueType.bindingPort:
        return toNumber(value);

      default:
        return value;
    }
  };

  const RenderField = params => {
    const {
      key,
      displayName,
      documentation,
      input,
      necessary,
      permission,
      tableKeys = [],
      list = [],
    } = params;

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
      ...fieldProps,
      key,
      list,
      name: key,
      tableKeys,
      label: displayName,
      helperText: documentation,
      component: input,
      disabled,
      required: necessary === 'REQUIRED',
      validate: validWithDef(params),
      parse: parseValueByType(def.valueType),
      type: def.valueType === valueType.boolean ? 'checkbox' : null,
    };

    if (ref) ensuredFieldProps.refs = ref;

    return <Field {...ensuredFieldProps} />;
  };

  const renderDefinitionField = () => {
    if (def.reference === 'NONE') {
      switch (def.valueType) {
        case valueType.string:
          return RenderField({ ...def, input: StringDef });

        case valueType.remotePort:
          return RenderField({ ...def, input: RemotePort });

        case valueType.int:
          return RenderField({ ...def, input: IntDef });

        case valueType.class:
          return RenderField({ ...def, input: ClassDef });

        case valueType.password:
          return RenderField({ ...def, input: Password });

        case valueType.positiveInt:
          return RenderField({ ...def, input: PositiveInt });

        case valueType.duration:
          return RenderField({ ...def, input: Duration });

        case valueType.bindingPort:
          return RenderField({ ...def, input: BindingPort, list: freePorts });

        case valueType.tags:
          return RenderField({ ...def, input: Tags });

        case valueType.jdbcTable:
          return RenderField({ ...def, input: JdbcTable });

        case valueType.table:
          return RenderField({ ...def, input: Table });

        case valueType.boolean:
          return RenderField({ ...def, input: BooleanDef });

        case valueType.long:
          return RenderField({ ...def, input: Long });

        case valueType.short:
          return RenderField({ ...def, input: Short });

        case valueType.double:
          return RenderField({ ...def, input: Double });

        case valueType.array:
          return RenderField({ ...def, input: ArrayDef });

        case valueType.positiveShort:
          return RenderField({ ...def, input: PositiveShort });

        case valueType.positiveLong:
          return RenderField({ ...def, input: PositiveLong });

        case valueType.positiveDouble:
          return RenderField({ ...def, input: PositiveDouble });

        case valueType.objectKeys:
          return RenderField({ ...def, input: ObjectKeys });

        case valueType.objectKey:
          return RenderField({ ...def, input: ObjectKey });

        default:
          return RenderField({ ...def, input: StringDef });
      }
    } else {
      switch (def.reference) {
        case 'TOPIC':
          return RenderField({
            ...def,
            input: Reference,
            list: finalTopics,
          });

        case 'FILE':
          return RenderField({ ...def, input: Reference, list: files });

        default:
          return RenderField({ ...def, input: Reference });
      }
    }
  };

  return renderDefinitionField();
};

export default RenderDefinition;
