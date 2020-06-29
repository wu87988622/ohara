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
import { filter, map, noop, some, values } from 'lodash';
import { v4 as uuid } from 'uuid';
import { Form, Field } from 'react-final-form';
import arrayMutators from 'final-form-arrays';
import { FieldArray } from 'react-final-form-arrays';
import Button from '@material-ui/core/Button';
import IconButton from '@material-ui/core/IconButton';
import AddIcon from '@material-ui/icons/Add';
import ClearIcon from '@material-ui/icons/Clear';

import * as hooks from 'hooks';
import { InputField, AutoComplete } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import { Tooltip } from 'components/common/Tooltip';
import { required, composeValidators } from 'utils/validate';
import Wrapper from './AutofillEditorStyles';
import { useSuggestiveKeys } from './autofillHooks';

export const MODE = {
  ADD: 'Add',
  EDIT: 'Edit',
  COPY: 'Copy',
};

const AutofillEditor = (props) => {
  const { isOpen, mode, onClose, data } = props;

  const workspace = hooks.useWorkspace();
  const updateWorkspace = hooks.useUpdateWorkspaceAction();

  const suggestiveKeys = useSuggestiveKeys();

  const duplicateDisplayName = (value) => {
    if (mode === MODE.EDIT && value === data.displayName) return;
    if (some(workspace?.settingFillings, { displayName: value })) {
      return `Name '${value}' already existed. Please use a different name`;
    }
  };

  const duplicateKey = (value, allValues) => {
    if (filter(allValues.settings, { key: value }).length > 1) {
      return `Key is duplicated`;
    }
  };

  const saveNew = (values) => {
    const newSettingFilling = {
      ...values,
      name: uuid(),
      lastModified: new Date(),
    };
    updateWorkspace({
      name: workspace.name,
      settingFillings: workspace?.settingFillings
        ? [...workspace.settingFillings, newSettingFilling]
        : [newSettingFilling],
    });
  };

  const save = (values) => {
    const newSettingFilling = { ...values, lastModified: new Date() };
    updateWorkspace({
      name: workspace.name,
      settingFillings: map(workspace?.settingFillings, (settingFilling) =>
        settingFilling.name === newSettingFilling.name
          ? newSettingFilling
          : settingFilling,
      ),
    });
  };

  const handleSubmit = (values, form) => {
    if (mode === MODE.ADD || mode === MODE.COPY) {
      saveNew(values);
    } else {
      save(values);
    }
    onClose();
    setTimeout(form.reset);
  };

  const createForm = (initialValues = {}) => (
    <Form
      initialValues={initialValues}
      mutators={{
        ...arrayMutators,
      }}
      onSubmit={handleSubmit}
      render={({
        form,
        form: {
          mutators: { push },
        },
        handleSubmit,
        invalid,
        pristine,
        submitting,
      }) => {
        return (
          <Dialog
            confirmDisabled={submitting || pristine || invalid}
            confirmText="SAVE"
            maxWidth="sm"
            onClose={() => {
              onClose();
              form.reset();
            }}
            onConfirm={handleSubmit}
            open={isOpen}
            title={`${mode} Autofill`}
          >
            <Wrapper>
              <Field
                autoFocus
                component={InputField}
                label="name"
                margin="normal"
                name="displayName"
                required
                type="text"
                validate={composeValidators(required, duplicateDisplayName)}
              />
              <FieldArray name="settings">
                {({ fields }) =>
                  fields.map((name, index) => (
                    <div className="setting" key={name}>
                      <Field
                        className="input"
                        closeIcon={<></>}
                        component={AutoComplete}
                        label="key"
                        margin="normal"
                        name={`${name}.key`}
                        options={suggestiveKeys}
                        required
                        type="text"
                        validate={composeValidators(required, duplicateKey)}
                      />
                      <Field
                        className="input"
                        component={InputField}
                        label="value"
                        margin="normal"
                        name={`${name}.value`}
                        type="text"
                      />
                      <Tooltip title="Delete the set of key, value">
                        <IconButton onClick={() => fields.remove(index)}>
                          <ClearIcon />
                        </IconButton>
                      </Tooltip>
                    </div>
                  ))
                }
              </FieldArray>
              <Tooltip placement="right" title="Add a set of key, value">
                <Button
                  className="add-button"
                  color="primary"
                  disabled={submitting || invalid}
                  onClick={() => push('settings', undefined)}
                  startIcon={<AddIcon />}
                  variant="outlined"
                >
                  ADD KEY
                </Button>
              </Tooltip>
            </Wrapper>
          </Dialog>
        );
      }}
    />
  );

  if (mode === MODE.ADD) return createForm();
  if (mode === MODE.EDIT) return createForm(data);
  if (mode === MODE.COPY) return createForm(data);
};

AutofillEditor.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  mode: PropTypes.oneOf(values(MODE)).isRequired,
  onClose: PropTypes.func,
  data: PropTypes.shape({
    name: PropTypes.string,
    displayName: PropTypes.string,
    settings: PropTypes.arrayOf(
      PropTypes.shape({
        key: PropTypes.string,
        value: PropTypes.oneOfType([
          PropTypes.string,
          PropTypes.number,
          PropTypes.bool,
        ]),
      }),
    ),
  }),
};

AutofillEditor.defaultProps = {
  onClose: noop,
  data: {},
};

export default AutofillEditor;
