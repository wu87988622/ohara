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

import React, { useState, useEffect } from 'react';
import toastr from 'toastr';
import PropTypes from 'prop-types';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import Checkbox from '@material-ui/core/Checkbox';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import DialogContent from '@material-ui/core/DialogContent';
import { Form, Field } from 'react-final-form';
import { Link } from 'react-router-dom';
import { isEmpty, get, isNull, split, isUndefined, uniq } from 'lodash';

import * as s from './styles';
import * as jarApi from 'api/jarApi';
import * as nodeApi from 'api/nodeApi';
import * as workerApi from 'api/workerApi';
import * as brokerApi from 'api/brokerApi';
import * as generate from 'utils/generate';
import * as MESSAGES from 'constants/messages';
import * as zookeeperApi from 'api/zookeeperApi';
import * as containerApi from 'api/containerApi';
import * as commonUtils from 'utils/commonUtils';
import * as URLS from 'constants/urls';
import { Label } from 'components/common/Form';
import { Button } from 'components/common/Mui/Form';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField } from 'components/common/Mui/Form';

const WorkerNewModal = props => {
  const [checkedNodes, setCheckedNodes] = useState([]);
  const [checkedFiles, setCheckedFiles] = useState([]);
  const [nodes, setNodes] = useState([]);
  const [jars, setJars] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  let workingServices = [];
  let plugins = [];

  const uploadJar = async file => {
    const res = await jarApi.createJar({
      file,
    });

    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      const jar = res.data.result;
      const newPlugins = [...plugins, jar];
      plugins = newPlugins;
      toastr.success(`${MESSAGES.PLUGIN_UPLOAD_SUCCESS} ${file.name}`);
    }
  };

  useEffect(() => {
    const fetchNodes = async () => {
      const res = await nodeApi.fetchNodes();
      const newNodes = get(res, 'data.result', null);
      if (!isNull(newNodes)) {
        setNodes(newNodes);
      }
    };
    fetchNodes();
  }, []);

  const handleNodeToggle = value => () => {
    const currentIndex = checkedNodes.indexOf(value);
    const newChecked = [...checkedNodes];

    if (currentIndex === -1) {
      newChecked.push(value);
    } else {
      newChecked.splice(currentIndex, 1);
    }

    setCheckedNodes(newChecked);
  };

  const handleFileToggle = value => () => {
    const currentIndex = checkedFiles.indexOf(value);
    const newChecked = [...checkedFiles];

    if (currentIndex === -1) {
      newChecked.push(value);
    } else {
      newChecked.splice(currentIndex, 1);
    }

    setCheckedFiles(newChecked);
  };

  const handleModalClose = () => {
    props.onClose();
  };

  const resetModal = form => {
    form.reset();
    setJars([]);
    setCheckedFiles([]);
    setCheckedNodes([]);
    handleModalClose();
    setIsLoading(false);
    plugins = [];
  };

  const validateServiceName = value => {
    if (isUndefined(value)) return '';

    if (value.match(/[^0-9a-z]/g)) {
      setErrorMessage('You only can use lower case letters and numbers');
    } else if (value.length > 30) {
      setErrorMessage('Must be between 1 and 30 characters long');
    } else {
      setErrorMessage('');
    }

    return value;
  };

  const waitDeleteService = async params => {
    const { service, name } = params;
    let { retryCount = 0 } = params;
    const maxRetry = 10;
    switch (service) {
      case 'zookeeper':
        const zkRes = await zookeeperApi.fetchZookeepers();
        const zkResult = zkRes.data.result.some(e => e.name === name);

        if (!zkResult) return;
        if (retryCount > maxRetry) {
          toastr.error(`Failed to delete zookeeper: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitDeleteService({ service, name, retryCount });
        return;

      case 'broker':
        const bkRes = await brokerApi.fetchBrokers();
        const bkResult = bkRes.data.result.some(e => e.name === name);

        if (!bkResult) return;
        if (retryCount > maxRetry) {
          toastr.error(`Failed to delete broker: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitDeleteService({ service, name, retryCount });
        return;

      case 'worker':
        const wkRes = await workerApi.fetchWorkers();
        const wkResult = wkRes.data.result.some(e => e.name === name);

        if (!wkResult) return;
        if (retryCount > maxRetry) {
          toastr.error(`Failed to delete worker: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitDeleteService({ service, name, retryCount });
        return;
      default:
        return;
    }
  };

  const saveService = params => {
    const newService = [...workingServices, params];
    workingServices = newService;
  };

  const deleteAllServices = async () => {
    plugins.forEach(plugin => {
      const { name, group } = plugin;
      jarApi.deleteJar({ name, workerClusterName: group });
    });
    const wks = workingServices.filter(working => working.service === 'worker');
    const bks = workingServices.filter(working => working.service === 'broker');
    const zks = workingServices.filter(
      working => working.service === 'zookeeper',
    );

    await Promise.all(
      wks.map(async wk => {
        await workerApi.deleteWorker(`${wk.name}`);
        await waitDeleteService({
          service: 'worker',
          name: wk.name,
        });
      }),
    );

    await Promise.all(
      bks.map(async bk => {
        await brokerApi.deleteBroker(`${bk.name}`);
        await waitDeleteService({
          service: 'broker',
          name: bk.name,
        });
      }),
    );

    await Promise.all(
      zks.map(async zk => {
        await zookeeperApi.deleteZookeeper(`${zk.name}`);
        await waitDeleteService({
          service: 'zookeeper',
          name: zk.name,
        });
      }),
    );
  };

  const createServices = async values => {
    setIsLoading(true);

    const nodeNames = checkedNodes;

    const maxRetry = 5;
    let retryCount = 0;
    const waitForServiceCreation = async clusterName => {
      if (!clusterName) throw new Error();

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

      if (containersAreReady) {
        retryCount = 0;
        return;
      } // exist successfully

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

    saveService({ service: 'zookeeper', name: zookeeperClusterName });

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

    saveService({ service: 'broker', name: brokerClusterName });

    const worker = await workerApi.createWorker({
      name: validateServiceName(values.name),
      nodeNames: nodeNames,
      plugins: plugins,
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

    saveService({ service: 'worker', name: workerClusterName });
  };

  const onSubmit = async (values, form) => {
    await Promise.all(checkedFiles.map(async file => await uploadJar(file)));

    try {
      await createServices(values);
    } catch (error) {
      // Ignore the error, that's handled in the API request handler

      toastr.error('Failed to create services, deleting the workspace…');
      await deleteAllServices();
      resetModal(form);
      return;
    }
    toastr.success(MESSAGES.SERVICE_CREATION_SUCCESS);
    props.onConfirm();
    resetModal(form);
  };

  const handleFileSelect = e => {
    const file = e.target.files[0];
    const newJars = [...jars];
    if (file) {
      newJars.push(file);
    }
    setJars(uniq(newJars));
  };

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, values }) => {
        return (
          <Dialog
            testId="new-workspace-modal"
            scroll="paper"
            loading={isLoading}
            title="New workspace"
            handelOpen={props.isActive}
            handelClose={() => {
              if (!submitting) resetModal(form);
            }}
            handleConfirm={handleSubmit}
            confirmDisabled={
              submitting ||
              pristine ||
              errorMessage !== '' ||
              checkedNodes.length === 0
            }
          >
            {isEmpty(nodes) ? (
              <s.StyledWarning
                data-testid="redirect-warning"
                text={
                  <>
                    {`You don't have any nodes available yet. But you can create one in `}
                    <Link to={URLS.NODES}>here</Link>
                  </>
                }
              />
            ) : (
              <s.StyledDialogDividers dividers>
                <s.StyledInputFile
                  id="fileInput"
                  accept=".jar"
                  type="file"
                  onChange={handleFileSelect}
                />
                <form onSubmit={handleSubmit}>
                  <DialogContent>
                    <Field
                      label="Name"
                      name="name"
                      component={InputField}
                      placeholder="cluster00"
                      data-testid="name-input"
                      disabled={submitting}
                      format={validateServiceName}
                      autoFocus
                      errorMessage={errorMessage}
                      error={errorMessage !== ''}
                    />
                  </DialogContent>
                  <s.StyledDialogContent>
                    <Label>Node List</Label>
                    <s.StyledPaper>
                      <List>
                        {nodes.map(node => {
                          const { name } = node;
                          return (
                            <ListItem
                              key={name}
                              dense
                              button
                              onClick={handleNodeToggle(name)}
                            >
                              <ListItemIcon>
                                <Checkbox
                                  color="primary"
                                  edge="start"
                                  checked={checkedNodes.indexOf(name) !== -1}
                                  tabIndex={-1}
                                  disableRipple
                                />
                              </ListItemIcon>
                              <ListItemText
                                id={name}
                                primary={name}
                                data-testid={name}
                              />
                            </ListItem>
                          );
                        })}
                      </List>
                    </s.StyledPaper>
                  </s.StyledDialogContent>
                  <s.StyledDialogContent>
                    <Label>Plugin List</Label>
                    <s.StyledPaper>
                      <List>
                        {jars.map(jar => {
                          const { name } = jar;
                          return (
                            <ListItem
                              key={name}
                              dense
                              button
                              onClick={handleFileToggle(jar)}
                            >
                              <ListItemIcon>
                                <Checkbox
                                  color="primary"
                                  edge="start"
                                  checked={checkedFiles.indexOf(jar) !== -1}
                                  tabIndex={-1}
                                  disableRipple
                                />
                              </ListItemIcon>
                              <ListItemText
                                id={jar}
                                primary={split(name, '.jar', 1)}
                              />
                            </ListItem>
                          );
                        })}
                        <s.StyledLabel htmlFor="fileInput">
                          <Button component="span" text="New Plugin" />
                        </s.StyledLabel>
                      </List>
                    </s.StyledPaper>
                  </s.StyledDialogContent>
                </form>
              </s.StyledDialogDividers>
            )}
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
