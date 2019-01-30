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
import { Form, Field } from 'react-final-form';
import toastr from 'toastr';
import { map } from 'lodash';

import * as _ from 'utils/commonUtils';
import * as workerApis from 'apis/workerApis';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { Label } from 'common/Form';
import * as MESSAGES from 'constants/messages';
import { InputField } from 'common/FormFields';

import NodeSelectModal from '../NodeSelectModal';
import PluginSelectModal from '../PluginSelectModal';
import * as s from './Styles';

const SELECT_NODE_MODAL = 'selectNodeModal';
const SELECT_PLUGIN_MODAL = 'selectPluginModal';

class WorkerNewModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
  };

  state = {
    activeModal: null,
  };

  handleModalClose = () => {
    this.props.onClose();
  };

  onSubmit = async (values, form) => {
    const { clientPort } = values;
    if (clientPort < 5000 || clientPort > 65535) {
      toastr.error(
        'Invalid port. The port has to be a value from 5000 to 65535.',
      );
      return;
    }
    const res = await workerApis.createWorker({
      ...values,
      plugins: map(values.plugins, 'id'),
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      form.reset();
      toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
      this.props.onConfirm();
      this.handleModalClose();
    }
  };

  /**
   * 只允許數字、字母大小寫
   *
   * @param value
   * @returns {string}
   */
  formatName = value =>
    value === undefined ? '' : value.replace(/[^0-9a-zA-Z]/g, '');

  render() {
    const { activeModal } = this.state;
    return (
      <Form
        onSubmit={this.onSubmit}
        initialValues={{}}
        render={({ handleSubmit, form, submitting, pristine, values }) => {
          const handleNodeSelected = nodeNames => {
            form.change('nodeNames', nodeNames);
          };

          const handlePluginSelected = plugins => {
            form.change('plugins', plugins);
          };

          return (
            <Modal
              title="New connect service"
              isActive={this.props.isActive}
              width="600px"
              handleCancel={() => {
                if (!submitting) {
                  form.reset();
                  this.handleModalClose();
                }
              }}
              handleConfirm={handleSubmit}
              confirmBtnText="Add"
              isConfirmDisabled={submitting || pristine}
              isConfirmWorking={submitting}
              showActions={true}
            >
              <form onSubmit={handleSubmit}>
                <Box shadow={false}>
                  <s.FormRow>
                    <s.FormCol width="26rem">
                      <Label>Service</Label>
                      <Field
                        name="name"
                        component={InputField}
                        width="24rem"
                        placeholder="cluster00"
                        data-testid="name-input"
                        disabled={submitting}
                        format={this.formatName}
                      />
                    </s.FormCol>
                    <s.FormCol width="8rem">
                      <Label>Port</Label>
                      <Field
                        name="clientPort"
                        component={InputField}
                        type="number"
                        min={5000}
                        max={65535}
                        width="8rem"
                        placeholder="5000"
                        data-testid="client-port-input"
                        disabled={submitting}
                      />
                    </s.FormCol>
                  </s.FormRow>

                  <s.FormRow margin="1rem 0 0 0">
                    <s.FormCol width="18rem">
                      <Label>Node List</Label>
                      <s.List width="16rem">
                        {values.nodeNames &&
                          values.nodeNames.map(nodeName => (
                            <s.ListItem key={nodeName}>{nodeName}</s.ListItem>
                          ))}
                        <s.AppendButton
                          text="Add node"
                          handleClick={e => {
                            e.preventDefault();
                            this.setState({ activeModal: SELECT_NODE_MODAL });
                          }}
                          disabled={submitting}
                        />
                      </s.List>
                    </s.FormCol>
                    <s.FormCol width="16rem">
                      <Label>Plugin List</Label>
                      <s.List width="16rem">
                        {values.plugins &&
                          values.plugins.map(plugin => (
                            <s.ListItem key={plugin.id}>
                              {plugin.name}
                            </s.ListItem>
                          ))}
                        <s.AppendButton
                          text="Add plugin"
                          handleClick={e => {
                            e.preventDefault();
                            this.setState({ activeModal: SELECT_PLUGIN_MODAL });
                          }}
                          disabled={submitting}
                        />
                      </s.List>
                    </s.FormCol>
                  </s.FormRow>
                </Box>
              </form>

              <NodeSelectModal
                isActive={activeModal === SELECT_NODE_MODAL}
                onClose={() => {
                  this.setState({ activeModal: null });
                }}
                onConfirm={nodeNames => {
                  this.setState({ activeModal: null });
                  handleNodeSelected(nodeNames);
                }}
                initNodeNames={values.nodeNames}
              />
              <PluginSelectModal
                isActive={activeModal === SELECT_PLUGIN_MODAL}
                onClose={() => {
                  this.setState({ activeModal: null });
                }}
                onConfirm={plugins => {
                  this.setState({ activeModal: null });
                  handlePluginSelected(plugins);
                }}
                initPluginIds={map(values.plugins, 'id')}
              />
            </Modal>
          );
        }}
      />
    );
  }
}

export default WorkerNewModal;
