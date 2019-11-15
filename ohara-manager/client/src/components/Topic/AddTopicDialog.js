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
import { useParams } from 'react-router-dom';
import { Form, Field } from 'react-final-form';
import { isEmpty, get } from 'lodash';

import * as topicApi from 'api/topicApi';
import { Dialog } from 'components/common/Dialog';
import { InputField } from 'components/common/Form';
import { useSnackbar } from 'context/SnackbarContext';
import { useAddTopic } from 'context/AddTopicContext';
import { useTopic } from 'context/TopicContext';
import { useWorkspace } from 'context/WorkspaceContext';

import {
  required,
  minNumber,
  validServiceName,
  composeValidators,
} from 'utils/validate';

const AddTopicDialog = () => {
  const { isOpen, setIsOpen } = useAddTopic();
  const [isSaving, setIsSaving] = useState(false);
  const showMessage = useSnackbar();

  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const currentWorkspace = findByWorkspaceName(workspaceName);
  const { doFetch: fetchTopics } = useTopic();

  const handleClose = () => setIsOpen(false);

  const onSubmit = async (values, form) => {
    const { name: topicName } = values;
    const { name: topicGroup } = currentWorkspace.settings;

    setIsSaving(true);
    const createTopicResponse = await topicApi.create({
      name: topicName,
      numberOfPartitions: Number(values.numberOfPartitions),
      numberOfReplications: Number(values.numberOfReplications),
      brokerClusterKey: {
        group: currentWorkspace.settings.group,
        name: currentWorkspace.settings.brokerClusterKey.name,
      },
      group: topicGroup,
    });

    const isCreated = !isEmpty(createTopicResponse);

    // Failed to create, show a custom error message here and keep the
    // dialog open
    if (!isCreated) {
      // After the error handling logic is done in https://github.com/oharastream/ohara/issues/3124
      // we can remove this custom message since it's handled higher up in the API layer
      showMessage(`Failed to add topic ${topicName}`);
      setIsSaving(false);
      return;
    }

    const startTopicResponse = await topicApi.start({
      name: topicName,
      group: topicGroup,
    });

    const isStarted = get(startTopicResponse, 'state', undefined);

    // Failed to start, show a custom error message here and keep the
    // dialog open
    if (!isStarted) {
      // After the error handling logic is done in https://github.com/oharastream/ohara/issues/3124
      // we can remove this custom message since it's handled higher up in the API layer
      showMessage(`Failed to start topic ${topicName}`);
      setIsSaving(false);
      return;
    }

    // Topic successsfully created, display success message,
    // update topic list and close the dialog
    showMessage(`Successfully added topic ${topicName}`);
    await fetchTopics(currentWorkspace.settings.name);

    setIsSaving(false);
    setTimeout(form.reset);
    handleClose();
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            open={isOpen}
            title="Add a new topic"
            handleClose={() => {
              form.reset();
              handleClose();
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
