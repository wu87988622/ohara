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
import { map } from 'lodash';
import { Field, reduxForm } from 'redux-form';
import Button from '@material-ui/core/Button';
import Paper from '@material-ui/core/Paper';

import { FORM } from 'const';
import * as hooks from 'hooks';
import InputField from 'components/common/Form/InputField';
import { checkDuplicate } from 'utils/validate';
import validate from './validate';

const SetupWorkspaceForm = (props) => {
  const {
    handleSubmit,
    previousStep,
    invalid,
    pristine,
    submitting,
    anyTouched,
    touch,
  } = props;

  const change = hooks.useReduxFormChangeAction();
  const defaultWorkspaceName = hooks.useUniqueWorkspaceName();
  const formValues = hooks.useReduxFormValues(FORM.CREATE_WORKSPACE);
  const workspaces = hooks.useAllWorkspaces();

  React.useEffect(() => {
    if (!formValues?.workspace?.name && !anyTouched) {
      change(FORM.CREATE_WORKSPACE, 'workspace.name', defaultWorkspaceName);
      // Reset `anyTouched` so we won't run it again when users are editing the form
      touch(FORM.CREATE_WORKSPACE);
    }
  }, [anyTouched, change, defaultWorkspaceName, formValues, touch]);

  return (
    <form onSubmit={handleSubmit}>
      <Paper className="fields">
        <Field
          autoFocus
          component={InputField}
          label="Workspace name"
          margin="normal"
          name="workspace.name"
          placeholder="workspace1"
          required
          type="text"
          validate={checkDuplicate(map(workspaces, 'name'))}
        />
      </Paper>
      <div className="buttons">
        <Button onClick={previousStep}>Back</Button>
        <Button
          color="primary"
          disabled={invalid || pristine || submitting}
          type="submit"
          variant="contained"
        >
          Next
        </Button>
      </div>
    </form>
  );
};

SetupWorkspaceForm.propTypes = {
  handleSubmit: PropTypes.func.isRequired,
  previousStep: PropTypes.func.isRequired,
  touch: PropTypes.func.isRequired,
  invalid: PropTypes.bool.isRequired,
  pristine: PropTypes.bool.isRequired,
  submitting: PropTypes.bool.isRequired,
  anyTouched: PropTypes.bool.isRequired,
};

export default reduxForm({
  form: FORM.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
  validate,
})(SetupWorkspaceForm);
