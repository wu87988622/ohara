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
import { Form, Field } from 'react-final-form';

import { Dialog } from 'components/common/Dialog';
import { InputField } from 'components/common/Form';
import { useTopicState, useTopicActions, useAddTopicDialog } from 'context';
import { useEventLog } from 'context/eventLog/eventLogHooks';
import {
  required,
  minNumber,
  validServiceName,
  composeValidators,
} from 'utils/validate';

const AddTopicDialog = () => {
  const { isFetching: isSaving } = useTopicState();
  const eventLog = useEventLog();
  const { createTopic } = useTopicActions();

  const { isOpen: isDialogOpen, close: closeDialog } = useAddTopicDialog();

  const onSubmit = async (values, form) => {
    const { name: topicName } = values;
    const res = await createTopic({
      name: topicName,
      numberOfPartitions: Number(values.numberOfPartitions),
      numberOfReplications: Number(values.numberOfReplications),
      tags: {
        isShared: true,
      },
    });
    if (!res.error) {
      eventLog.info(`Successfully created topic ${topicName}.`);
      setTimeout(form.reset);
      closeDialog();
    }
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            open={isDialogOpen}
            title="Add a new topic"
            handleClose={() => {
              form.reset();
              closeDialog();
            }}
            loading={isSaving}
            handleConfirm={handleSubmit}
            confirmBText="ADD"
            confirmDisabled={submitting || pristine || invalid}
          >
            <form onSubmit={handleSubmit}>
              <Field
                type="text"
                label="Topic name"
                name="name"
                component={InputField}
                placeholder="mytopic"
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
                placeholder="1"
                inputProps={{
                  min: '1',
                  step: '1',
                }}
                validate={minNumber(1)}
              />
              <Field
                type="number"
                label="Replication factor"
                name="numberOfReplications"
                margin="normal"
                component={InputField}
                placeholder="1"
                inputProps={{
                  min: '1',
                  step: '1',
                }}
                validate={minNumber(1)}
              />
            </form>
          </Dialog>
        );
      }}
    />
  );
};

export default AddTopicDialog;
