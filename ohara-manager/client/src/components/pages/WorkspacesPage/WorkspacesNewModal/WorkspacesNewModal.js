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
import { map, isEmpty, truncate, get } from 'lodash';
import { Form, Field } from 'react-final-form';

import * as zookeeperApi from 'api/zookeeperApi';
import * as workerApi from 'api/workerApi';
import * as brokerApi from 'api/brokerApi';
import * as containerApi from 'api/containerApi';
import * as MESSAGES from 'constants/messages';
import * as generate from 'utils/generate';
import * as s from './styles';
import * as commonUtils from 'utils/commonUtils';
import NodeSelectModal from '../NodeSelectModal';
import PluginSelectModal from '../PluginSelectModal';
import { Box } from 'components/common/Layout';
import { Label } from 'components/common/Form';
import { InputField } from 'components/common/Mui/Form';
import { Dialog } from 'components/common/Mui/Dialog';

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

  createServices = async values => {
    const { nodeNames } = values;

    const maxRetry = 5;
    let retryCount = 0;
    const waitForServiceCreation = async clusterName => {
      if (!clusterName) return;

      const res = await containerApi.fetchContainers(clusterName);
      const containers = get(res, 'data.result[0].containers', null);

      let containersAreReady = false;
      if (!isEmpty(containers)) {
        containersAreReady = containers.every(
          container => container.state === 'RUNNING',
        );
      }

      if (retryCount > maxRetry)
        throw new Error(`Couldn't get the container state!`);

      if (containersAreReady) return; // exist successfully

      retryCount++;
      await commonUtils.sleep(2000);
      await waitForServiceCreation(clusterName);
    };

    const zookeeper = await zookeeperApi.createZookeeper({
      name: generate.serviceName(),
      clientPort: generate.port(),
      peerPort: generate.port(),
      electionPort: generate.port(),

      // TODO: #1632 there's a couple of rules when creating services
      // - Rule one: if the nodes supplied by users are less than 3. Creates a zookeeper and
      // the equal amount of brokers and workers of nodes
      // - Rule two: on the other hand, if users provide more than 3 nodes. We create 3 zookeepers
      // and equal amount of brokers and workers of nodes
      // due to a bug in #696. ohara is currently unable to create more than three zookeepers. So we're
      // now specifying only one zookeeper when creating services. `nodeNames[0]`👇
      // and once the bug is fixed we will then apply the rule two in frontend :)
      nodeNames: [nodeNames[0]],
    });

    const zookeeperClusterName = get(zookeeper, 'data.result.name');
    await waitForServiceCreation(zookeeperClusterName);

    const broker = await brokerApi.createBroker({
      name: generate.serviceName(),
      zookeeperClusterName,
      clientPort: generate.port(),
      exporterPort: generate.port(),
      jmxPort: generate.port(),
      nodeNames,
    });

    const brokerClusterName = get(broker, 'data.result.name');
    await waitForServiceCreation(brokerClusterName);

    const worker = await workerApi.createWorker({
      ...values,
      name: this.validateServiceName(values.name),
      plugins: values.plugins,
      jmxPort: generate.port(),
      clientPort: generate.port(),
      brokerClusterName,
      groupId: generate.serviceName(),
      configTopicName: generate.serviceName(),
      offsetTopicName: generate.serviceName(),
      statusTopicName: generate.serviceName(),
    });

    const workerClusterName = get(worker, 'data.result.name');
    await waitForServiceCreation(workerClusterName);
  };

  onSubmit = async (values, form) => {
    if (!this.validateNodeNames(values.nodeNames)) return;

    try {
      await this.createServices(values);
    } catch (error) {
      // Ignore the error

      toastr.error('Failed to create workspace!');
      this.resetModal(form);
      return;
    }

    toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
    this.props.onConfirm();
    this.resetModal(form);
  };

  resetModal = form => {
    form.reset();
    this.handleModalClose();
  };

  validateNodeNames = nodes => {
    if (isEmpty(nodes)) {
      toastr.error('You should at least supply a node name');
      return false;
    }

    return true;
  };

  validateServiceName = value => {
    if (!value) return '';

    // validating rules: numbers, lowercase letter and up to 30 characters
    return truncate(value.replace(/[^0-9a-z]/g, ''), {
      length: 30,
      omission: '',
    });
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
            <Dialog
              title="New workspace"
              handelOpen={this.props.isActive}
              handelClose={() => {
                if (!submitting) this.resetModal(form);
              }}
              handleConfirm={handleSubmit}
              confirmBtnText="Add"
              isConfirmDisabled={submitting || pristine}
              isConfirmWorking={submitting}
              showActions={true}
              testId="new-workspace-modal"
            >
              {() => {
                return (
                  <>
                    <form onSubmit={handleSubmit}>
                      <Box shadow={false}>
                        <s.FormRow>
                          <s.FormCol width="26rem">
                            <Field
                              label="Name"
                              id="service-input"
                              name="name"
                              component={InputField}
                              width="24rem"
                              placeholder="cluster00"
                              data-testid="name-input"
                              disabled={submitting}
                              format={this.validateServiceName}
                              autoFocus
                              helperText={
                                <div>
                                  <p>
                                    1. You can use lower case letters and
                                    numbers
                                  </p>
                                  <p>
                                    2. Must be between 1 and 30 characters long
                                  </p>
                                </div>
                              }
                            />
                          </s.FormCol>
                        </s.FormRow>

                        <s.FormRow margin="1rem 0 0 0">
                          <s.FormCol width="18rem">
                            <Label>Node List</Label>
                            <s.List width="16rem">
                              {values.nodeNames &&
                                values.nodeNames.map(nodeName => (
                                  <s.ListItem key={nodeName}>
                                    {nodeName}
                                  </s.ListItem>
                                ))}
                              <s.AppendButton
                                text="Add node"
                                handleClick={e => {
                                  e.preventDefault();
                                  this.setState({
                                    activeModal: SELECT_NODE_MODAL,
                                  });
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
                                  <s.ListItem key={plugin.group}>
                                    {plugin.name}
                                  </s.ListItem>
                                ))}
                              <s.AppendButton
                                text="Add plugin"
                                handleClick={e => {
                                  e.preventDefault();
                                  this.setState({
                                    activeModal: SELECT_PLUGIN_MODAL,
                                  });
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
                      initPluginIds={map(values.plugins, 'group')}
                    />
                  </>
                );
              }}
            </Dialog>
          );
        }}
      />
    );
  }
}

export default WorkerNewModal;
