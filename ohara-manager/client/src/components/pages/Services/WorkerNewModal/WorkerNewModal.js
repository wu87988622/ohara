import React from 'react';
import PropTypes from 'prop-types';
import { Form, Field } from 'react-final-form';
import toastr from 'toastr';
import { map } from 'lodash';

import * as _ from 'utils/commonUtils';
import * as workerApis from 'apis/workerApis';
import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import { FormGroup, Label } from 'common/Form';
import * as MESSAGES from 'constants/messages';
import InputField from 'pages/NodePage/InputField';

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
              title="New Worker Service"
              isActive={this.props.isActive}
              width="600px"
              handleCancel={() => {
                if (!submitting) {
                  form.reset();
                  this.handleModalClose();
                }
              }}
              handleConfirm={handleSubmit}
              confirmBtnText="Save"
              isConfirmDisabled={submitting || pristine}
              isConfirmWorking={submitting}
              showActions={true}
            >
              <form onSubmit={handleSubmit}>
                <Box shadow={false}>
                  <FormGroup data-testid="name">
                    <Label>Service name</Label>
                    <Field
                      name="name"
                      component={InputField}
                      width="14rem"
                      placeholder="connect-cluster-00"
                      data-testid="name-input"
                      disabled={submitting}
                    />
                  </FormGroup>
                  <FormGroup data-testid="client-port">
                    <Label>Port</Label>
                    <Field
                      name="clientPort"
                      component={InputField}
                      type="number"
                      min={0}
                      max={65535}
                      width="14rem"
                      placeholder="1021"
                      data-testid="client-port-input"
                      disabled={submitting}
                    />
                  </FormGroup>
                  <s.FormRow>
                    <s.FormCol width="18rem">
                      <Label>Node List</Label>
                      <s.List width="14rem">
                        {values.nodeNames &&
                          values.nodeNames.map(nodeName => (
                            <s.ListItem key={nodeName}>{nodeName}</s.ListItem>
                          ))}
                        <s.AppendButton
                          text="Append node"
                          handleClick={e => {
                            e.preventDefault();
                            this.setState({ activeModal: SELECT_NODE_MODAL });
                          }}
                          disabled={submitting}
                        />
                      </s.List>
                    </s.FormCol>
                    <s.FormCol width="18rem">
                      <Label>Plugin List</Label>
                      <s.List width="16rem">
                        {values.plugins &&
                          values.plugins.map(plugin => (
                            <s.ListItem key={plugin.id}>
                              {plugin.name}
                            </s.ListItem>
                          ))}
                        <s.AppendButton
                          text="Append plugin"
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
