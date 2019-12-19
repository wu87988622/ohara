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

import BindingPort from './BindingPort';
import BooleanDef from './BooleanDef';
import Duration from './Duration';
import IntDef from './IntDef';
import JdbcTable from './JdbcTable';
import Password from './Password';
import Port from './Port';
import Reference from './Reference';
import StringDef from './StringDef';
import Table from './Table';
import Tags from './Tags';
import PositiveInt from './PositiveInt';
import ClassDef from './ClassDef';
import Long from './Long';
import Short from './Short';
import Doubel from './Double';
import ArrayDef from './ArrayDef';
import ObjectKey from './ObjectKey';
import ObjectKeys from './ObjectKeys';
import PositiveDouble from './PositiveDouble';
import PositiveLong from './PositiveLong';
import PositiveShort from './PositiveShort';
import { validWithDef } from 'utils/validate';
import { READ_ONLY, CREATE_ONLY, EDITABLE } from './Permission';

const RenderDefinition = props => {
  const { def, topics, files, defType, ref = {} } = props;

  const RenderField = params => {
    const {
      key,
      displayName,
      documentation,
      input,
      necessary,
      permission,
      tableKeys = [],
    } = params;

    let disabled = false;

    switch (permission) {
      case READ_ONLY:
        disabled = true;
        break;

      case CREATE_ONLY:
        disabled = defType === CREATE_ONLY ? false : true;
        break;

      case EDITABLE:
        disabled = defType === EDITABLE ? false : true;
        disabled = true;
        break;

      default:
        break;
    }

    return (
      <Field
        refs={ref}
        tableKeys={tableKeys}
        key={key}
        name={key}
        label={displayName}
        helperText={documentation}
        component={input}
        disabled={disabled}
        required={necessary === 'REQUIRED'}
        validate={validWithDef(params)}
      />
    );
  };

  const renderDefinitionField = () => {
    if (def.reference === 'NONE') {
      switch (def.valueType) {
        case 'STRING':
          return RenderField({ ...def, input: StringDef });

        case 'PORT':
          return RenderField({ ...def, input: Port });

        case 'INT':
          return RenderField({ ...def, input: IntDef });

        case 'CLASS':
          return RenderField({ ...def, input: ClassDef });

        case 'PASSWORD':
          return RenderField({ ...def, input: Password });

        case 'POSITIVE_INT':
          return RenderField({ ...def, input: PositiveInt });

        case 'DURATION':
          return RenderField({ ...def, input: Duration });

        case 'BINDING_PORT':
          return RenderField({ ...def, input: BindingPort });

        case 'TAGS':
          return RenderField({ ...def, input: Tags });

        case 'JDBC_TABLE':
          return RenderField({ ...def, input: JdbcTable });

        case 'TABLE':
          return RenderField({ ...def, input: Table });

        case 'BOOLEAN':
          return RenderField({ ...def, input: BooleanDef });

        case 'LONG':
          return RenderField({ ...def, input: Long });

        case 'SHORT':
          return RenderField({ ...def, input: Short });

        case 'DOUBLE':
          return RenderField({ ...def, input: Doubel });

        case 'ARRAY':
          return RenderField({ ...def, input: ArrayDef });

        case 'POSITIVE_SHORT':
          return RenderField({ ...def, input: PositiveShort });

        case 'POSITIVE_LONG':
          return RenderField({ ...def, input: PositiveLong });

        case 'POSITIVE_DOUBLE':
          return RenderField({ ...def, input: PositiveDouble });

        case 'OBJECT_KEYS':
          return RenderField({ ...def, input: ObjectKeys });

        case 'OBJECT_KEY':
          return RenderField({ ...def, input: ObjectKey });

        default:
          return RenderField({ ...def, input: StringDef });
      }
    } else {
      // eslint-disable-next-line default-case
      switch (def.reference) {
        case 'TOPIC':
          return RenderField({ ...def, input: Reference, topics });

        case 'FILE':
          return RenderField({ ...def, input: Reference, files });

        case 'ZOOKEEPER_CLUSTER':
        case 'BROKER_CLUSTER':
        case 'WORKER_CLUSTER':
          return;

        default:
          return RenderField({ ...def, input: Reference });
      }
    }
  };

  const definitionField = renderDefinitionField();

  return { definitionField, ref };
};

export default RenderDefinition;
