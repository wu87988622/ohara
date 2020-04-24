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
import { get, max, size } from 'lodash';
import { Form, Field } from 'react-final-form';

import { Dialog } from 'components/common/Dialog';
import { InputField } from 'components/common/Form';
import {
  required,
  minNumber,
  maxNumber,
  validServiceName,
  composeValidators,
} from 'utils/validate';

const TopicCreateDialog = ({ broker, isOpen, onClose, onConfirm }) => {
  const minNumberOfPartitions = 1;
  const minNumberOfReplications = 1;
  const maxNumberOfReplications = max([
    minNumberOfReplications,
    size(get(broker, 'aliveNodes')),
  ]);

  const handleSubmit = (values, form) => {
    const finalValues = {
      ...values,
      numberOfPartitions: Number(values?.numberOfPartitions),
      numberOfReplications: Number(values?.numberOfReplications),
      tags: {
        isShared: true,
      },
    };
    onConfirm(finalValues);
    onClose();
    setTimeout(form.reset);
  };

  return (
    <Form
      onSubmit={handleSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            confirmDisabled={submitting || pristine || invalid}
            confirmText="CREATE"
            onClose={() => {
              form.reset();
              onClose();
            }}
            onConfirm={handleSubmit}
            open={isOpen}
            title="Create topic"
          >
            <form onSubmit={handleSubmit}>
              <Field
                type="text"
                label="Topic name"
                name="name"
                component={InputField}
                placeholder="topic"
                margin="normal"
                validate={composeValidators(required, validServiceName)}
                autoFocus
                required
              />
              <Field
                type="number"
                label="Partitions"
                name="numberOfPartitions"
                margin="normal"
                component={InputField}
                placeholder={`${minNumberOfPartitions}`}
                inputProps={{
                  min: `${minNumberOfPartitions}`,
                  step: '1',
                }}
                validate={minNumber(minNumberOfPartitions)}
              />
              <Field
                type="number"
                label="Replication factor"
                name="numberOfReplications"
                margin="normal"
                component={InputField}
                placeholder={`${minNumberOfReplications}`}
                inputProps={{
                  min: `${minNumberOfReplications}`,
                  step: '1',
                }}
                validate={composeValidators(
                  minNumber(minNumberOfReplications),
                  maxNumber(maxNumberOfReplications),
                )}
              />
            </form>
          </Dialog>
        );
      }}
    />
  );
};

TopicCreateDialog.propTypes = {
  broker: PropTypes.shape({
    aliveNodes: PropTypes.array,
  }),
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onConfirm: PropTypes.func.isRequired,
};

TopicCreateDialog.defaultProps = {
  broker: null,
  onClose: () => {},
};

export default TopicCreateDialog;
