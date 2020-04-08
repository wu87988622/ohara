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
    // we only convert the necessary values to correct type
    return isNumberType(type) ? toNumber(value) : value;
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
      type: def.valueType === Type.BOOLEAN ? 'checkbox' : null,
    };

    if (ref) ensuredFieldProps.refs = ref;

    return <Field {...ensuredFieldProps} />;
  };

  const renderDefinitionField = () => {
    if (def.reference === ReferenceEnum.NONE) {
      switch (def.valueType) {
        case Type.STRING:
          return RenderField({ ...def, input: StringDef });

        case Type.REMOTE_PORT:
          return RenderField({ ...def, input: RemotePort });

        case Type.INT:
          return RenderField({ ...def, input: IntDef });

        case Type.CLASS:
          return RenderField({ ...def, input: ClassDef });

        case Type.PASSWORD:
          return RenderField({ ...def, input: Password });

        case Type.POSITIVE_INT:
          return RenderField({ ...def, input: PositiveInt });

        case Type.DURATION:
          return RenderField({ ...def, input: Duration });

        case Type.BINDING_PORT:
          return RenderField({ ...def, input: BindingPort, list: freePorts });

        case Type.TAGS:
          return RenderField({ ...def, input: Tags });

        case Type.JDBC_TABLE:
          return RenderField({ ...def, input: JdbcTable });

        case Type.TABLE:
          return RenderField({ ...def, input: Table });

        case Type.BOOLEAN:
          return RenderField({ ...def, input: BooleanDef });

        case Type.LONG:
          return RenderField({ ...def, input: Long });

        case Type.SHORT:
          return RenderField({ ...def, input: Short });

        case Type.DOUBLE:
          return RenderField({ ...def, input: Double });

        case Type.ARRAY:
          return RenderField({ ...def, input: ArrayDef });

        case Type.POSITIVE_SHORT:
          return RenderField({ ...def, input: PositiveShort });

        case Type.POSITIVE_LONG:
          return RenderField({ ...def, input: PositiveLong });

        case Type.POSITIVE_DOUBLE:
          return RenderField({ ...def, input: PositiveDouble });

        case Type.OBJECT_KEYS:
          return RenderField({ ...def, input: ObjectKeys });

        case Type.OBJECT_KEY:
          return RenderField({ ...def, input: ObjectKey });

        default:
          return RenderField({ ...def, input: StringDef });
      }
    } else {
      switch (def.reference) {
        case ReferenceEnum.TOPIC:
          return RenderField({
            ...def,
            input: Reference,
            list: finalTopics,
          });

        case ReferenceEnum.FILE:
          return RenderField({ ...def, input: Reference, list: files });

        default:
          return RenderField({ ...def, input: Reference });
      }
    }
  };

  return renderDefinitionField();
};

export default RenderDefinition;
