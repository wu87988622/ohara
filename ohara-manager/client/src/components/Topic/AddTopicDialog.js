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
import _ from 'lodash';
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
import * as context from 'context';
import * as hooks from 'hooks';

const AddTopicDialog = props => {
  const { uniqueId } = props;
  const currentBroker = hooks.useBroker();
  const createAndStartTopic = hooks.useCreateAndStartTopicAction();
  const {
    isOpen: isDialogOpen,
    close: closeDialog,
  } = context.useAddTopicDialog();
  const replicationFactor = _.get(currentBroker, 'aliveNodes', []);

  const onSubmit = async (values, form) => {
    const { name: topicName } = values;
    createAndStartTopic({
      name: topicName,
      displayName: topicName,
      isShared: true,
      numberOfPartitions: Number(values.numberOfPartitions),
      numberOfReplications: Number(values.numberOfReplications),
    });
    setTimeout(form.reset);
    closeDialog();
  };

  return (
    <Form
      initialValues={{}}
      onSubmit={onSubmit}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            confirmBText="ADD"
            confirmDisabled={submitting || pristine || invalid}
            onClose={() => {
              form.reset();
              closeDialog();
            }}
            onConfirm={handleSubmit}
            open={isDialogOpen}
            title="Add a new topic"
          >
            <form onSubmit={handleSubmit}>
              <Field
                autoFocus
                component={InputField}
                id={`add-topic-name-${uniqueId}`}
                label="Topic name"
                margin="normal"
                name="name"
                placeholder="mytopic"
                required
                type="text"
                validate={composeValidators(required, validServiceName)}
              />
              <Field
                component={InputField}
                id={`add-topic-partitions-${uniqueId}`}
                inputProps={{
                  min: '1',
                  step: '1',
                }}
                label="Partitions"
                margin="normal"
                name="numberOfPartitions"
                placeholder="1"
                type="number"
                validate={minNumber(1)}
              />
              <Field
                component={InputField}
                id={`add-topic-replication-factor-${uniqueId}`}
                inputProps={{
                  min: '1',
                  step: '1',
                }}
                label="Replication factor"
                margin="normal"
                name="numberOfReplications"
                placeholder="1"
                type="number"
                validate={composeValidators(
                  minNumber(1),
                  maxNumber(replicationFactor.length),
                )}
              />
            </form>
          </Dialog>
        );
      }}
    />
  );
};

AddTopicDialog.propTypes = {
  uniqueId: PropTypes.string.isRequired,
};

export default AddTopicDialog;
