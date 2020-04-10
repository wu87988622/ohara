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
import { reduxForm } from 'redux-form';
import Button from '@material-ui/core/Button';
import Paper from '@material-ui/core/Paper';

import { FORM } from 'const';

const SetupWorkerForm = props => {
  const { handleSubmit, previousStep } = props;
  return (
    <form onSubmit={handleSubmit}>
      <Paper className="fields"></Paper>
      <div className="buttons">
        <Button onClick={previousStep}>Back</Button>
        <Button variant="contained" color="primary" onClick={handleSubmit}>
          Next
        </Button>
      </div>
    </form>
  );
};

SetupWorkerForm.propTypes = {
  handleSubmit: PropTypes.func.isRequired,
  previousStep: PropTypes.func.isRequired,
};

export default reduxForm({
  form: FORM.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
})(SetupWorkerForm);