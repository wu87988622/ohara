import React from 'react';
import PropTypes from 'prop-types';
import { Form, Field } from 'react-final-form';
import toastr from 'toastr';

import * as _ from 'utils/commonUtils';
import * as topicApis from 'apis/topicApis';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import * as MESSAGES from 'constants/messages';
import InputField from '../../NodePage/InputField';

class TopicNewModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  handleClose = () => {
    this.props.onClose();
  };

  onSubmit = async (values, form) => {
    const res = await topicApis.createTopic(values);
    const isSuccess = _.get(res, 'data.isSuccess', false);
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
              showActions={true}
            >
              <form onSubmit={handleSubmit}>
                <Box shadow={false}>
                  <FormGroup data-testid="name">
                    <Label>Topic name</Label>
                    <Field
                      name="name"
                      component={InputField}
                      width="100%"
                      placeholder="kafka-cluster"
                      data-testid="name-input"
                    />
                  </FormGroup>
                  <FormGroup data-testid="partitions">
                    <Label>Partitions</Label>
                    <Field
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
                    <Label>Replication factor</Label>
                    <Field
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
