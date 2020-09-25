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
import { omit } from 'lodash';
import PropTypes from 'prop-types';
import { Field, reduxForm } from 'redux-form';
import { Dialog } from 'components/common/Dialog';
import { InputField, AutoComplete } from 'components/common/Form';
import { Form } from 'const';
import * as hooks from 'hooks';

const VolumeEditDialog = ({
  isOpen,
  onClose,
  onConfirm,
  volume,
  nodeNames,
}) => {
  if (!volume) return null;
  const { path, nodeNames: oldNodeNames, name } = volume;
  const { displayName, usedBy } = volume.tags;
  const formValues = hooks.useReduxFormValues(Form.EDIT_VOLUME);

  return (
    <Dialog
      confirmText="CREATE"
      onClose={() => {
        onClose();
      }}
      onConfirm={() => {
        onConfirm({
          name,
          path,
          nodeNames: oldNodeNames,
          ...omit(formValues, ['tags']),
          tags: {
            displayName,
            usedBy,
            ...formValues?.tags,
          },
        });
        onClose();
      }}
      open={isOpen}
      title={`Edit the volume of ${displayName}`}
    >
      <form onSubmit={onConfirm}>
        <Field
          component={InputField}
          defaultValue={displayName}
          helperText="custom volume name"
          id="Volume name"
          label="Name"
          name="displayName"
          placeholder="volume"
          required
          type="text"
        />
        <Field
          component={InputField}
          defaultValue={path}
          disabled
          id="Volume path"
          label="Path"
          margin="normal"
          name="path"
          type="text"
        />
        <Field
          component={AutoComplete}
          getOptionValue={(value) => value}
          helperText="nodes"
          id="Nodes"
          initializeValue={oldNodeNames}
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

VolumeEditDialog.propTypes = {
  volume: PropTypes.shape({
    name: PropTypes.string,
    path: PropTypes.string,
    nodeNames: PropTypes.string,
    tags: PropTypes.shape({
      displayName: PropTypes.string,
      usedBy: PropTypes.string,
    }),
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

VolumeEditDialog.defaultProps = {
  onClose: () => {},
};
export default reduxForm({
  form: Form.EDIT_VOLUME,
})(VolumeEditDialog);
