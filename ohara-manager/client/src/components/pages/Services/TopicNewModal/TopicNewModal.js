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
import toastr from 'toastr';
import { Form, Field } from 'react-final-form';
import { get } from 'lodash';

import * as topicApi from 'api/topicApi';
import * as MESSAGES from 'constants/messages';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import { InputField } from 'common/FormFields';

class TopicNewModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  state = {
    isSaveBtnWorking: false,
  };

  handleClose = () => {
    this.props.onClose();
  };

  onSubmit = async (values, form) => {
    this.setState({ isSaveBtnWorking: true });
    const res = await topicApi.createTopic(values);
    this.setState({ isSaveBtnWorking: false });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      toastr.success(MESSAGES.TOPIC_CREATION_SUCCESS);
      this.props.onConfirm();
      this.handleClose();
    }
  };

  render() {
    return (
      <Form
        onSubmit={this.onSubmit}
        initialValues={{}}
        render={({ handleSubmit, form, submitting, pristine, invalid }) => {
          return (
            <Modal
              title="New topic"
              isActive={this.props.isActive}
              width="320px"
              handleCancel={() => {
                form.reset();
                this.handleClose();
              }}
              handleConfirm={handleSubmit}
              confirmBtnText="Save"
              isConfirmDisabled={submitting || pristine || invalid}
              isConfirmWorking={this.state.isSaveBtnWorking}
              showActions={true}
            >
              <form onSubmit={handleSubmit}>
                <Box shadow={false}>
                  <FormGroup data-testid="name">
                    <Label htmlFor="topicInput">Topic name</Label>
                    <Field
                      id="topicInput"
                      name="name"
                      component={InputField}
                      width="100%"
                      placeholder="Kafka Topic"
                      data-testid="name-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="partitions">
                    <Label htmlFor="partInput">Partitions</Label>
                    <Field
                      id="partInput"
                      name="numberOfPartitions"
                      component={InputField}
                      type="number"
                      min={1}
                      max={1000}
                      width="50%"
                      placeholder="1"
                      data-testid="partitions-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="replications">
                    <Label htmlFor="repInput">Replication factor</Label>
                    <Field
                      id="repInput"
                      name="numberOfReplications"
                      component={InputField}
                      type="number"
                      min={1}
                      max={1000}
                      width="50%"
                      placeholder="3"
                      data-testid="replications-input"
                    />
                  </FormGroup>
                </Box>
              </form>
            </Modal>
          );
        }}
      />
    );
  }
}

export default TopicNewModal;
