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
import { get, isArray } from 'lodash';
import PropTypes from 'prop-types';
import { Field, reduxForm } from 'redux-form';

import { Dialog } from 'components/common/Dialog';
import {
  InputField,
  AutoComplete,
  ValidateInputField,
} from 'components/common/Form';
import { Form } from 'const';
import * as hooks from 'hooks';
import validateVolumePath from '../Workspace/CreateWorkspace/validateVolumePath';

const VolumeCreateDialog = ({
  isOpen,
  onClose,
  onConfirm,
  nodeNames,
  usedBy,
}) => {
  const formValues = hooks.useReduxFormValues(Form.CREATE_VOLUME);
  const validateVolumePathState = hooks.useValidateVolumePath();
  const workspaceNodeNames = {
    workspace: { nodeNames: nodeNames.map((node) => node.hostname) },
  };
  const validateVolumePathAction = hooks.useValidateVolumePathAction();
  const isValidate = () => {
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

  return (
    <Dialog
      confirmDisabled={
        !isValidate() ||
        !get(formValues, 'displayName', undefined) ||
        !isArray(get(formValues, 'nodeNames', undefined))
      }
      confirmText="CREATE"
      onClose={() => {
        onClose();
      }}
      onConfirm={() => {
        onConfirm(formValues);
        onClose();
      }}
      open={isOpen}
      title="Create volume"
    >
      <form onSubmit={onConfirm}>
        <Field
          autoFocus
          component={InputField}
          helperText="custom volume name"
          id="Volume name"
          label="Name"
          name="displayName"
          placeholder="volume"
          required
          type="text"
        />
        <Field
          component={ValidateInputField}
          hasError={!isValidate()}
          helperText={helperText()}
          label="Volume path"
          margin="normal"
          name="path"
          onBlur={validateVolumePath(
            validateVolumePathAction,
            workspaceNodeNames,
          )}
          placeholder="/home/ohara/workspace1"
          required
          type="text"
        />
        <Field
          component={AutoComplete}
          getOptionValue={(value) => value}
          helperText="nodes"
          id="Nodes"
          label="Nodes"
          margin="normal"
          multiple={true}
          name="nodeNames"
          onBlur={(e) => {
            e.preventDefault();
          }}
          options={nodeNames.map((node) => node.hostname)}
          required
          type="text"
        />
        <Field
          component={InputField}
          defaultValue={usedBy}
          disabled
          id="Volume usedBy"
          label="Used By"
          margin="normal"
          name="usedBy"
          type="text"
        />
      </form>
    </Dialog>
  );
};

VolumeCreateDialog.propTypes = {
  broker: PropTypes.shape({
    aliveNodes: PropTypes.array,
  }),
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
  nodeNames: PropTypes.arrayOf(
    PropTypes.shape({
      hostname: PropTypes.string,
    }),
  ).isRequired,
  usedBy: PropTypes.string.isRequired,
};

VolumeCreateDialog.defaultProps = {
  onClose: () => {},
};

export default reduxForm({
  form: Form.CREATE_VOLUME,
})(VolumeCreateDialog);
