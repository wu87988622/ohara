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
import { sortBy } from 'lodash';
import PropTypes from 'prop-types';
import { Form, Field } from 'react-final-form';

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

const RenderDefinitions = props => {
  const { Definitions = [], onSubmit, topics = [] } = props;
  const displayDefinitions = Definitions.filter(def => !def.internal);

  const renderField = params => {
    const {
      key,
      displayName,
      documentation,
      input,
      necessary,
      editable,
    } = params;
    return (
      <Field
        key={key}
        name={key}
        label={displayName}
        helperText={documentation}
        component={input}
        disabled={!editable}
        required={necessary === 'REQUIRED'}
      />
    );
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form }) => {
        return (
          <form onSubmit={handleSubmit}>
            {sortBy(displayDefinitions, 'orderInGroup').map(def => {
              if (def.reference === 'NONE') {
                switch (def.valueType) {
                  case 'STRING':
                    return renderField({ ...def, input: StringDef });

                  case 'PORT':
                    return renderField({ ...def, input: Port });

                  case 'INT':
                    return renderField({ ...def, input: IntDef });

                  case 'CLASS':
                    return renderField({ ...def, input: ClassDef });

                  case 'PASSWORD':
                    return renderField({ ...def, input: Password });

                  case 'POSITIVE_INT':
                    return renderField({ ...def, input: PositiveInt });

                  case 'DURATION':
                    return renderField({ ...def, input: Duration });

                  case 'BINDING_PORT':
                    return renderField({ ...def, input: BindingPort });

                  case 'TAGS':
                    return renderField({ ...def, input: Tags });

                  case 'JDBC_TABLE':
                    return renderField({ ...def, input: JdbcTable });

                  case 'TABLE':
                    return <Table {...def} />;

                  case 'BOOLEAN':
                    return renderField({ ...def, input: BooleanDef });

                  case 'LONG':
                    return renderField({ ...def, input: Long });

                  default:
                    return renderField({ ...def, input: StringDef });
                }
              } else {
                // eslint-disable-next-line default-case
                switch (def.reference) {
                  case 'TOPIC':
                    return renderField({ ...def, input: Reference, topics });

                  case 'FILE':
                    return renderField({ ...def, input: Reference });

                  case 'WORKER_CLUSTER':
                    return renderField({ ...def, input: Reference });

                  default:
                    return renderField({ ...def, input: Reference });
                }
              }
            })}
          </form>
        );
      }}
    />
  );
};

RenderDefinitions.propTypes = {
  Definitions: PropTypes.array,
  topics: PropTypes.array,
  onSubmit: PropTypes.func,
};

export default RenderDefinitions;
