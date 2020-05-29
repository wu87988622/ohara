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
import PropTypes from 'prop-types';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import { Form, Field } from 'react-final-form';

import * as validate from 'utils/validate';
import * as hooks from 'hooks';

const AddPipelineDialog = ({ isOpen, setIsOpen }) => {
  const createPipeline = hooks.useCreatePipelineAction();
  const pipelines = hooks.usePipelines();

  const onSubmit = ({ pipelineName: name }, form) => {
    createPipeline({ name });
    setTimeout(form.reset);
    setIsOpen(false);
  };

  return (
    <Form
      initialValues={{}}
      onSubmit={onSubmit}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => (
        <Dialog
          confirmDisabled={submitting || pristine || invalid}
          maxWidth="xs"
          onClose={() => {
            setIsOpen(false);
            form.reset();
          }}
          onConfirm={handleSubmit}
          open={isOpen}
          testId="new-pipeline-dialog"
          title="Add a new pipeline"
        >
          <form onSubmit={handleSubmit}>
            <Field
              autoFocus
              component={InputField}
              label="Pipeline name"
              name="pipelineName"
              placeholder="pipeline1"
              required
              type="text"
              validate={validate.composeValidators(
                validate.required,
                validate.minLength(2),
                validate.maxLength(20),
                validate.validServiceName,
                validate.checkDuplicate(
                  pipelines.map(pipeline => pipeline.name),
                ),
              )}
            />
          </form>
        </Dialog>
      )}
    />
  );
};

AddPipelineDialog.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  setIsOpen: PropTypes.func.isRequired,
};

export default AddPipelineDialog;
