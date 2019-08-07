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
import { Form, Field } from 'react-final-form';
import { get } from 'lodash';
import DialogContent from '@material-ui/core/DialogContent';

import * as MESSAGES from 'constants/messages';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField } from 'components/common/Mui/Form';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const TopicNewModal = props => {
  const [isSaving, setIsSaving] = useState(false);
  const { getData: topicRes, postApi: createTopic } = useApi.usePostApi(
    URL.TOPIC_URL,
  );
  const { putApi: startTopic } = useApi.usePutApi(URL.TOPIC_URL);
  const { showMessage } = useSnackbar();

  const handleClose = () => {
    props.onClose();
  };

  const onSubmit = async (values, form) => {
    setIsSaving(true);

    await createTopic({
      name: values.name,
      numberOfPartitions: Number(values.numberOfPartitions),
      numberOfReplications: Number(values.numberOfReplications),
      brokerClusterName: props.brokerClusterName,
    });
    await startTopic(`/${values.name}/start`);
    setIsSaving(false);
    const isSuccess = get(topicRes(), 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      showMessage(MESSAGES.TOPIC_CREATION_SUCCESS);
      props.onConfirm();
      handleClose();
    }
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, invalid }) => {
        return (
          <Dialog
            title="New topic"
            open={props.isActive}
            handelClose={() => {
              form.reset();
              handleClose();
            }}
            loading={isSaving}
            handleConfirm={handleSubmit}
            confirmBtnText="Save"
            confirmDisabled={submitting || pristine || invalid}
            testId="topic-new-modal"
          >
            <form onSubmit={handleSubmit}>
              <DialogContent>
                <Field
                  label="Topic name"
                  id="topicInput"
                  name="name"
                  component={InputField}
                  placeholder="Kafka Topic"
                  data-testid="name-input"
                  autoFocus
                />
              </DialogContent>
              <DialogContent>
                <Field
                  label="Partitions"
                  id="partInput"
                  name="numberOfPartitions"
                  component={InputField}
                  type="number"
                  placeholder="1"
                  data-testid="partitions-input"
                />
              </DialogContent>
              <DialogContent>
                <Field
                  label="Replication factor"
                  id="repInput"
                  name="numberOfReplications"
                  component={InputField}
                  type="number"
                  placeholder="1"
                  data-testid="replications-input"
                />
              </DialogContent>
            </form>
          </Dialog>
        );
      }}
    />
  );
};

TopicNewModal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  brokerClusterName: PropTypes.string.isRequired,
};

export default TopicNewModal;
