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
import toastr from 'toastr';
import { map, isEmpty, truncate, get } from 'lodash';
import { Form, Field } from 'react-final-form';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import Checkbox from '@material-ui/core/Checkbox';
import Paper from '@material-ui/core/Paper';

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
import { Label } from 'components/common/Form';
import { InputField } from 'components/common/Mui/Form';
import { Dialog } from 'components/common/Mui/Dialog';

const SELECT_NODE_MODAL = 'selectNodeModal';
const SELECT_PLUGIN_MODAL = 'selectPluginModal';

const WorkerNewModal = props => {
  const [activeModal, setActiveModal] = useState(null);
  const [checked, setChecked] = React.useState([]);

  const handleToggle = value => () => {
    const currentIndex = checked.indexOf(value);
    const newChecked = [...checked];

    if (currentIndex === -1) {
      newChecked.push(value);
    } else {
      newChecked.splice(currentIndex, 1);
    }

    setChecked(newChecked);
    console.log(newChecked);
  };

  const handleModalClose = () => {
    props.onClose();
  };

  const resetModal = form => {
    form.reset();
    handleModalClose();
  };

  const validateNodeNames = nodes => {
    if (isEmpty(nodes)) {
      toastr.error('You should at least supply a node name');
      return false;
    }

    return true;
  };

  const validateServiceName = value => {
    if (!value) return '';

    // validating rules: numbers, lowercase letter and up to 30 characters
    return truncate(value.replace(/[^0-9a-z]/g, ''), {
      length: 30,
      omission: '',
    });
  };

  const createServices = async values => {
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
      name: validateServiceName(values.name),
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

  const onSubmit = async (values, form) => {
    if (!validateNodeNames(values.nodeNames)) return;

    try {
      await createServices(values);
    } catch (error) {
      // Ignore the error

      toastr.error('Failed to create workspace!');
      resetModal(form);
      return;
    }

    toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
    props.onConfirm();
    resetModal(form);
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, values }) => {
        const handleNodeSelected = nodeNames => {
          console.log(nodeNames);
          // form.change('nodeNames', nodeNames);
        };

        const handlePluginSelected = plugins => {
          form.change('plugins', plugins);
        };

        return (
          <Dialog
            title="New workspace"
            handelOpen={props.isActive}
            handelClose={() => {
              if (!submitting) resetModal(form);
            }}
            handleConfirm={handleSubmit}
            confirmDisabled={submitting || pristine}
          >
            {() => {
              return (
                <>
                  <form onSubmit={handleSubmit}>
                    <DialogContent>
                      <DialogContentText>
                        <Field
                          label="Name"
                          id="service-input"
                          name="name"
                          component={InputField}
                          width="24rem"
                          placeholder="cluster00"
                          data-testid="name-input"
                          disabled={submitting}
                          format={validateServiceName}
                          autoFocus
                          helperText={
                            <div>
                              <p>
                                1. You can use lower case letters and numbers
                              </p>
                              <p>2. Must be between 1 and 30 characters long</p>
                            </div>
                          }
                        />
                      </DialogContentText>
                    </DialogContent>
                    <s.StyledDialogConten>
                      <DialogContentText>
                        <Label>Node List</Label>
                        <Paper>
                          <List>
                            {['node00', 'node01', 'node02', 'node03'].map(
                              value => {
                                const labelId = `checkbox-list-label-${value}`;

                                return (
                                  <ListItem
                                    key={value}
                                    dense
                                    button
                                    onClick={handleToggle(value)}
                                  >
                                    <ListItemIcon>
                                      <Checkbox
                                        color="primary"
                                        edge="start"
                                        checked={checked.indexOf(value) !== -1}
                                        tabIndex={-1}
                                        disableRipple
                                        inputProps={{
                                          'aria-labelledby': labelId,
                                        }}
                                      />
                                    </ListItemIcon>
                                    <ListItemText
                                      id={labelId}
                                      primary={value}
                                    />
                                  </ListItem>
                                );
                              },
                            )}
                          </List>
                        </Paper>
                      </DialogContentText>
                    </s.StyledDialogConten>
                    <s.StyledDialogConten>
                      <DialogContentText>
                        <Label>Plugin List</Label>
                        <s.List>
                          {values.plugins &&
                            values.plugins.map(plugin => (
                              <s.ListItem key={plugin.group}>
                                {plugin.name}
                              </s.ListItem>
                            ))}
                          <s.StyledIconButton
                            zIndex="5000"
                            color="primary"
                            onClick={e => {
                              e.preventDefault();
                              setActiveModal(SELECT_PLUGIN_MODAL);
                            }}
                            disabled={submitting}
                          >
                            <s.ActionIcon className="fas fa-file-upload" />
                          </s.StyledIconButton>
                        </s.List>
                      </DialogContentText>
                    </s.StyledDialogConten>
                  </form>

                  <NodeSelectModal
                    isActive={activeModal === SELECT_NODE_MODAL}
                    onClose={() => {
                      setActiveModal(null);
                    }}
                    onConfirm={nodeNames => {
                      setActiveModal(null);
                      handleNodeSelected(nodeNames);
                    }}
                    initNodeNames={values.nodeNames}
                  />

                  <PluginSelectModal
                    isActive={activeModal === SELECT_PLUGIN_MODAL}
                    onClose={() => {
                      setActiveModal(null);
                    }}
                    onConfirm={plugins => {
                      setActiveModal(null);
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
};

WorkerNewModal.propTypes = {
  isActive: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
};

export default WorkerNewModal;
