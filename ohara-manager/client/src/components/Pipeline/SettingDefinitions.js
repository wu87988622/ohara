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

import React, { useRef, Fragment } from 'react';
import { capitalize } from 'lodash';
import PropTypes from 'prop-types';
import { Form } from 'react-final-form';
import Typography from '@material-ui/core/Typography';

import RenderDefinition from 'components/common/Definitions/RenderDefinition';
import { EDITABLE } from 'components/common/Definitions/Permission';

const RenderDefinitions = props => {
  const {
    Definitions = [],
    initialValues = {},
    onSubmit,
    topics = [],
    files = [],
  } = props;

  const formRef = useRef(null);
  const formHandleSubmit = () =>
    formRef.current.dispatchEvent(new Event('submit'));
  const refs = {};

  const displayDefinitions = Definitions.filter(def => !def.internal);

  const RenderForm = (
    <Form
      onSubmit={onSubmit}
      initialValues={initialValues}
      render={({ handleSubmit, form }) => {
        return (
          <form onSubmit={handleSubmit} ref={formRef}>
            {displayDefinitions.map(defs => {
              const title = defs[0].group;
              return (
                <Fragment key={title}>
                  <Typography variant="h4">{capitalize(title)}</Typography>
                  {defs.map(def => {
                    const ref = React.createRef();
                    const {
                      definitionField,
                      ref: currentRef,
                    } = RenderDefinition({
                      def,
                      topics,
                      files,
                      ref,
                      defType: EDITABLE,
                    });

                    refs[def.key] = currentRef;

                    return definitionField;
                  })}
                </Fragment>
              );
            })}
          </form>
        );
      }}
    />
  );

  return { RenderForm, formHandleSubmit, refs };
};

RenderDefinitions.propTypes = {
  Definitions: PropTypes.array,
  topics: PropTypes.array,
  onSubmit: PropTypes.func,
  id: PropTypes.string,
};

export default RenderDefinitions;
