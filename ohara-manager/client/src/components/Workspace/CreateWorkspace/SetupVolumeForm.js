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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { Field, reduxForm } from 'redux-form';
import Button from '@material-ui/core/Button';
import Paper from '@material-ui/core/Paper';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';

import { Form } from 'const';
import * as hooks from 'hooks';
import { ValidateInputField } from 'components/common/Form';
import validateVolumePath from './validateVolumePath';

const SetupVolumeForm = (props) => {
  const { handleSubmit, previousStep, invalid, pristine, submitting } = props;

  const [checked, setChecked] = useState(false);
  const change = hooks.useReduxFormChangeAction();
  const formValues = hooks.useReduxFormValues(Form.CREATE_WORKSPACE);
  const validateVolumePathAction = hooks.useValidateVolumePathAction();
  const validateVolumePathState = hooks.useValidateVolumePath();

  const isValidate = () => {
    if (!checked) {
      return true;
    }
    if (
      validateVolumePathState?.isValidate &&
      !validateVolumePathState?.validating
    ) {
      return true;
    } else if (
      !validateVolumePathState?.isValidate &&
      !validateVolumePathState?.validating
    ) {
      return false;
    }
  };

  const helperText = () => {
    if (!validateVolumePathState) {
      return 'Please provide the path and we will create this folder on each node you select. Other service volumes will be placed under this path.';
    }
    if (validateVolumePathState.validating) {
      return 'Validate Volume Path ...';
    }
    if (!isValidate()) {
      return 'This volume path is illegal';
    }
    if (isValidate()) {
      return 'This volume path is legitimate';
    }
  };
  const handleCheck = () => {
    if (checked) {
      setChecked(false);
      change(Form.CREATE_WORKSPACE, 'volume.path', undefined);
    } else {
      setChecked(true);
    }
  };

  return (
    <form onSubmit={handleSubmit}>
      <Paper className="fields">
        <FormControlLabel
          control={
            <Checkbox
              checked={checked}
              color="primary"
              onChange={handleCheck}
            />
          }
          label="Enable volumes"
        />
        <Field
          component={ValidateInputField}
          disabled={!checked}
          hasError={!isValidate()}
          helperText={helperText()}
          label="Volume path"
          margin="normal"
          name="volume.path"
          onBlur={validateVolumePath(validateVolumePathAction, formValues)}
          placeholder="/home/ohara/workspace1"
          type="text"
        />
      </Paper>
      <div className="buttons">
        <Button onClick={previousStep}>BACK</Button>
        <Button
          color="primary"
          disabled={invalid || pristine || submitting || !isValidate()}
          type="submit"
          variant="contained"
        >
          NEXT
        </Button>
      </div>
    </form>
  );
};

SetupVolumeForm.propTypes = {
  handleSubmit: PropTypes.func.isRequired,
  previousStep: PropTypes.func.isRequired,
  invalid: PropTypes.bool.isRequired,
  pristine: PropTypes.bool.isRequired,
  submitting: PropTypes.bool.isRequired,
};

export default reduxForm({
  form: Form.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
})(SetupVolumeForm);
