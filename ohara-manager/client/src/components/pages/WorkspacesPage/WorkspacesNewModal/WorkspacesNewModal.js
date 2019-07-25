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
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import Checkbox from '@material-ui/core/Checkbox';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import DialogContent from '@material-ui/core/DialogContent';
import { Form, Field } from 'react-final-form';
import { Link } from 'react-router-dom';
import { isEmpty, get, split, isUndefined, uniq } from 'lodash';

import * as s from './styles';
import * as generate from 'utils/generate';
import * as MESSAGES from 'constants/messages';
import * as commonUtils from 'utils/commonUtils';
import * as URLS from 'constants/urls';
import { Label } from 'components/common/Form';
import { Progress } from 'components/common/Mui/Feedback';
import { Button } from 'components/common/Mui/Form';
import { Dialog } from 'components/common/Mui/Dialog';
import { InputField } from 'components/common/Mui/Form';
import useSnackbar from 'components/context/Snackbar/useSnackbar';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

const WorkerNewModal = props => {
  const [checkedNodes, setCheckedNodes] = useState([]);
  const [checkedFiles, setCheckedFiles] = useState([]);
  const [jars, setJars] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [activeStep, setActiveStep] = useState(0);
  const [deleteType, setDeleteType] = useState(false);
  const { showMessage } = useSnackbar();
  const { getData: worker, postApi: createWorker } = useApi.usePostApi(
    URL.WORKER_URL,
  );
  const { getData: broker, postApi: createBroker } = useApi.usePostApi(
    URL.BROKER_URL,
  );
  const { getData: zookeeper, postApi: createZookeeper } = useApi.usePostApi(
    URL.ZOOKEEPER_URL,
  );
  const { getData: jarRes, uploadApi } = useApi.useUploadApi(URL.FILE_URL);
  const { DELETE: deleteJar } = useApi.useDeleteApi(URL.FILE_URL);
  const { getData: getWorkers, getApi: fetchWorkers } = useApi.useGetApi(
    URL.WORKER_URL,
  );
  const { getData: getBrokers, getApi: fetchBrokers } = useApi.useGetApi(
    URL.BROKER_URL,
  );
  const { getData: getZookeepers, getApi: fetchZookeepers } = useApi.useGetApi(
    URL.ZOOKEEPER_URL,
  );
  const { getData: containerRes, getApi: fetchContainer } = useApi.useGetApi(
    URL.CONTAINER_URL,
  );
  const { deleteApi: deleteWorker } = useApi.useDeleteApi(URL.WORKER_URL);
  const { deleteApi: deleteBroker } = useApi.useDeleteApi(URL.BROKER_URL);
  const { deleteApi: deleteZookeeper } = useApi.useDeleteApi(URL.ZOOKEEPER_URL);
  const { data: nodes } = useApi.useFetchApi(URL.NODE_URL);
  const { putApi: putZookeeper } = useApi.usePutApi(URL.ZOOKEEPER_URL);
  const { putApi: putBroker } = useApi.usePutApi(URL.BROKER_URL);

  let workingServices = [];
  let plugins = [];

  const uploadJar = async file => {
    const formData = new FormData();
    formData.append('file', file);
    await uploadApi(formData);

    const isSuccess = get(jarRes(), 'data.isSuccess', false);
    if (isSuccess) {
      const jar = jarRes().data.result;
      const newPlugins = [...plugins, jar];
      plugins = newPlugins;
    }
  };

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
    setActiveStep(0);
    setDeleteType(false);
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
        await fetchZookeepers();
        const zkResult = getZookeepers().data.result.some(e => e.name === name);

        if (!zkResult) return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to delete zookeeper: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitDeleteService({ service, name, retryCount });
        return;

      case 'broker':
        await fetchBrokers();
        const bkResult = getBrokers().data.result.some(e => e.name === name);

        if (!bkResult) return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to delete broker: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitDeleteService({ service, name, retryCount });
        return;

      case 'worker':
        await fetchWorkers();
        const wkResult = getWorkers().data.result.some(e => e.name === name);

        if (!wkResult) return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to delete worker: ${name}`);
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

  const waitStopService = async params => {
    const { service, name } = params;
    let { retryCount = 0 } = params;
    const maxRetry = 10;

    switch (service) {
      case 'zookeeper':
        await fetchZookeepers(name);
        const zkResult = get(getZookeepers(), 'data.result.state', undefined);

        if (isUndefined(zkResult)) return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to stop zookeeper: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitStopService({ service, name, retryCount });
        return;

      case 'broker':
        fetchBrokers(name);
        const bkResult = get(getBrokers(), 'data.result.state', 'stop');

        if (bkResult === 'stop') return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to stop broker: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitStopService({ service, name, retryCount });
        return;

      case 'worker':
        await fetchWorkers(name);
        const wkResult = get(getWorkers(), 'data.result.state', 'stop');

        if (wkResult === 'stop') return;
        if (retryCount > maxRetry) {
          showMessage(`Failed to delete worker: ${name}`);
          return;
        }

        await commonUtils.sleep(3000);
        retryCount++;
        await waitStopService({ service, name, retryCount });
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
    setDeleteType(true);
    plugins.forEach(plugin => {
      const { name, group } = plugin;
      deleteJar(`${name}?group=${group}`);
    });

    const wks = workingServices.filter(working => working.service === 'worker');
    const bks = workingServices.filter(working => working.service === 'broker');
    const zks = workingServices.filter(
      working => working.service === 'zookeeper',
    );
    setActiveStep(3);
    await Promise.all(
      wks.map(async wk => {
        await deleteWorker(`${wk.name}`);
        await waitDeleteService({
          service: 'worker',
          name: wk.name,
        });
      }),
    );
    setActiveStep(2);
    await Promise.all(
      bks.map(async bk => {
        await putBroker(`/${bk.name}/stop`);
        await waitStopService({
          service: 'broker',
          name: bk.name,
        });
        await deleteBroker(`${bk.name}`);
        await waitDeleteService({
          service: 'broker',
          name: bk.name,
        });
      }),
    );

    setActiveStep(1);
    await Promise.all(
      zks.map(async zk => {
        await putZookeeper({ type: `${zk.name}/stop` });
        await waitStopService({
          service: 'zookeeper',
          name: zk.name,
        });
        await deleteZookeeper(`${zk.name}`);
        await waitDeleteService({
          service: 'zookeeper',
          name: zk.name,
        });
      }),
    );
    setActiveStep(0);
  };

  const createServices = async values => {
    const nodeNames = checkedNodes;

    const maxRetry = 5;
    let retryCount = 0;

    const waitForServiceCreation = async clusterName => {
      if (!clusterName) throw new Error();

      await fetchContainer(clusterName);
      const containers = get(containerRes(), 'data.result[0].containers', null);
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

    const zookeeperName = generate.serviceName();
    await createZookeeper({
      name: zookeeperName,
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

    await putZookeeper({ type: `${zookeeperName}/start` });
    const zookeeperClusterName = get(zookeeper(), 'data.result.name');
    await waitForServiceCreation(zookeeperClusterName);
    setActiveStep(1);

    saveService({ service: 'zookeeper', name: zookeeperClusterName });

    await createBroker({
      name: generate.serviceName(),
      zookeeperClusterName,
      clientPort: generate.port(),
      exporterPort: generate.port(),
      jmxPort: generate.port(),
      nodeNames,
    });

    const brokerClusterName = get(broker(), 'data.result.name');
    await waitForServiceCreation(brokerClusterName);
    setActiveStep(2);

    saveService({ service: 'broker', name: brokerClusterName });

    const jars = plugins.map(jar => {
      return {
        name: jar.name,
        group: jar.group,
      };
    });

    await createWorker({
      name: validateServiceName(values.name),
      nodeNames,
      jars,
      jmxPort: generate.port(),
      clientPort: generate.port(),
      brokerClusterName,
      groupId: generate.serviceName(),
      configTopicName: generate.serviceName(),
      offsetTopicName: generate.serviceName(),
      statusTopicName: generate.serviceName(),
    });

    const workerClusterName = get(worker(), 'data.result.name');
    await waitForServiceCreation(workerClusterName);
    setActiveStep(3);

    saveService({ service: 'worker', name: workerClusterName });
  };

  const onSubmit = async (values, form) => {
    setIsLoading(true);
    await Promise.all(checkedFiles.map(async file => await uploadJar(file)));

    try {
      await createServices(values);
    } catch (error) {
      // Ignore the error, that's handled in the API request handler

      await deleteAllServices();
      resetModal(form);
      return;
    }
    showMessage(MESSAGES.SERVICE_CREATION_SUCCESS);
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

  const steps = ['Zookeeper', 'Broker', 'Worker'];

  return (
    <Form
      onSubmit={onSubmit}
      initialValues={{}}
      render={({ handleSubmit, form, submitting, pristine, values }) => {
        return (
          <>
            <Dialog
              testId="new-workspace-modal"
              scroll="paper"
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
                          {get(nodes, 'data.result', []).map(node => {
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
            <Progress
              open={isLoading}
              steps={steps}
              activeStep={activeStep}
              deleteType={deleteType}
              deleteTitle="Failed to create services, deleting the workspace…"
            />
          </>
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
