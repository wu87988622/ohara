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
  validServiceName,
  composeValidators,
  checkDuplicate,
} from 'utils/validate';

const TopicCreateDialog = ({ broker, isOpen, onClose, onConfirm, topics }) => {
  const minNumberOfPartitions = 1;
  const minNumberOfReplications = 1;
  const maxNumberOfReplications = max([
    minNumberOfReplications,
    size(get(broker, 'aliveNodes')),
  ]);

  const validate = values => {
    const { numberOfPartitions, numberOfReplications } = values;

    const errors = {};
    if (numberOfPartitions < minNumberOfPartitions) {
      errors.numberOfPartitions = `Partitions must be greater than or equal to ${minNumberOfPartitions}`;
    }
    if (numberOfReplications < minNumberOfReplications) {
      errors.numberOfReplications = `Replication factor must be greater than or equal to ${minNumberOfReplications}`;
    }
    if (numberOfReplications > maxNumberOfReplications) {
      errors.numberOfReplications = `Replication factor should be less than or equal to ${maxNumberOfReplications} broker as there is only ${maxNumberOfReplications} available in the current workspace`;
    }
    return errors;
  };

  return (
    <Form
      onSubmit={(values, form) => {
        onConfirm(values, form).then(() => {
          form.reset();
          onClose();
        });
      }}
      initialValues={{}}
      validate={validate}
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
                id="Topic name"
                name="name"
                component={InputField}
                placeholder="topic"
                margin="normal"
                validate={composeValidators(
                  required,
                  validServiceName,
                  checkDuplicate(topics.map(topic => topic.name)),
                )}
                autoFocus
                required
              />
              <Field
                type="number"
                label="Partitions"
                id="Partitions"
                name="numberOfPartitions"
                margin="normal"
                component={InputField}
                placeholder={`${minNumberOfPartitions}`}
                inputProps={{
                  min: `${minNumberOfPartitions}`,
                  step: '1',
                }}
                validate={required}
              />
              <Field
                type="number"
                label="Replication factor"
                id="Replication factor"
                name="numberOfReplications"
                margin="normal"
                component={InputField}
                placeholder={`${minNumberOfReplications}`}
                inputProps={{
                  min: `${minNumberOfReplications}`,
                  step: '1',
                }}
                validate={required}
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
  topics: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
    }),
  ).isRequired,
};

TopicCreateDialog.defaultProps = {
  broker: null,
  onClose: () => {},
};

export default TopicCreateDialog;
