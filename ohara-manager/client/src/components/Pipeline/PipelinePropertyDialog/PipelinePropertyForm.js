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

import React, { useRef, Fragment, createRef } from 'react';
import { capitalize, flatten } from 'lodash';
import PropTypes from 'prop-types';
import { Form } from 'react-final-form';
import Typography from '@material-ui/core/Typography';

import RenderDefinition from 'components/common/Definitions/RenderDefinition';
import { EDITABLE } from 'components/common/Definitions/Permission';

const scrollIntoViewOption = { behavior: 'smooth', block: 'start' };

const PipelinePropertyForm = React.forwardRef((props, ref) => {
  const {
    definitions = [],
    files = [],
    freePorts,
    initialValues = {},
    onSubmit,
    topics = [],
  } = props;
  const formRef = useRef(null);
  const fieldRefs = {};

  // Apis
  const apis = {
    change: (key, value) => formRef.current.change(key, value),
    getDefinitions: () => flatten(definitions),
    scrollIntoView: key => {
      if (fieldRefs[key].current)
        fieldRefs[key].current.scrollIntoView(scrollIntoViewOption);
    },
    submit: () => formRef.current.submit(),
    values: () => formRef.current.getState().values,
  };

  React.useImperativeHandle(ref, () => apis);

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={initialValues}
      render={({ handleSubmit, form }) => {
        formRef.current = form;
        return (
          <form onSubmit={handleSubmit}>
            {definitions.map(defs => {
              const title = defs[0].group;
              return (
                <Fragment key={title}>
                  <Typography variant="h4">{capitalize(title)}</Typography>
                  {defs
                    .filter(def => !def.internal)
                    .map(def => {
                      fieldRefs[def.key] = createRef();
                      return RenderDefinition({
                        def,
                        topics,
                        files,
                        ref: fieldRefs[def.key],
                        defType: EDITABLE,
                        freePorts,
                      });
                    })}
                </Fragment>
              );
            })}
          </form>
        );
      }}
    />
  );
});

PipelinePropertyForm.propTypes = {
  definitions: PropTypes.array,
  files: PropTypes.array,
  freePorts: PropTypes.array,
  initialValues: PropTypes.object,
  onSubmit: PropTypes.func,
  topics: PropTypes.array,
};

export default PipelinePropertyForm;
