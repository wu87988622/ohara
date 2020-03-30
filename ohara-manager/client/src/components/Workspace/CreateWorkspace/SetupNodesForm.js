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
import { size } from 'lodash';
import { Field, reduxForm } from 'redux-form';
import Button from '@material-ui/core/Button';
import Paper from '@material-ui/core/Paper';

import { FORM } from 'const';
import NodesField from './NodesField';

const required = value => {
  return size(value) > 0 ? undefined : 'This is a required field';
};

const SetupNodesForm = props => {
  const { handleSubmit, previousStep, invalid, pristine, submitting } = props;
  return (
    <form onSubmit={handleSubmit}>
      <Paper className="fields">
        <Field
          name="workspace.nodeNames"
          label="Workspace nodes"
          component={NodesField}
          required
          validate={required}
        />
      </Paper>
      <div className="buttons">
        <Button onClick={previousStep}>Back</Button>
        <Button
          type="submit"
          variant="contained"
          color="primary"
          disabled={invalid || pristine || submitting}
        >
          Next
        </Button>
      </div>
    </form>
  );
};

SetupNodesForm.propTypes = {
  handleSubmit: PropTypes.func.isRequired,
  previousStep: PropTypes.func.isRequired,
  invalid: PropTypes.bool.isRequired,
  pristine: PropTypes.bool.isRequired,
  submitting: PropTypes.bool.isRequired,
};

export default reduxForm({
  form: FORM.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
})(SetupNodesForm);
