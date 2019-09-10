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
import PropTypes from 'prop-types';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import { get, divide, floor, isUndefined } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as commonUtils from 'utils/commonUtils';
import * as utils from '../WorkspacesDetailPageUtils';
import { Progress } from 'components/common/Mui/Feedback';
import { Button } from 'components/common/Mui/Form';
import { DeleteDialog } from 'components/common/Mui/Dialog';
import { SortTable } from 'components/common/Mui/Table';
import { Main, ActionIcon, StyledInputFile, StyledLabel } from './styles';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

const Plugins = props => {
  const { worker, workerRefetch } = props;
  const [jarNameToBeDeleted, setJarNameToBeDeleted] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [activeStep, setActiveStep] = useState(0);
  const {
    data: jarsRes,
    isLoading: jarIsLoading,
    refetch: jarRefetch,
  } = useApi.useFetchApi(URL.FILE_URL);
  const jars = get(jarsRes, 'data.result', []);

  const [workerActions, setWorkerActions] = useState([]);

  const { getData: getJarRes, uploadApi } = useApi.useUploadApi(URL.FILE_URL);
  const { getData: getDeleteRes, deleteApi } = useApi.useDeleteApi(
    URL.FILE_URL,
  );
  const { getData: pipelineRes, getApi: getPipeline } = useApi.useGetApi(
    URL.PIPELINE_URL,
  );
  const { putApi: putWorker } = useApi.usePutApi(URL.WORKER_URL);
  const { putApi: putConnector } = useApi.usePutApi(URL.CONNECTOR_URL);
  const { waitApi } = useApi.useWaitApi();
  const { showMessage } = useSnackbar();

  useEffect(() => {
    const { jarInfos } = worker;
    const defaultAction = jarInfos.map(jarInfo => {
      return {
        name: jarInfo.name,
        size: jarInfo.size,
        lastModified: jarInfo.lastModified,
        group: jarInfo.group,
        type: 'READY',
      };
    });

    setWorkerActions(defaultAction);
  }, [worker]);

  const uploadJar = async file => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('group', `${worker.name}-plugin`);
    await uploadApi(formData);

    const isSuccess = get(getJarRes(), 'data.isSuccess', false);
    if (isSuccess) {
      showMessage(MESSAGES.PLUGIN_UPLOAD_SUCCESS);
    }
    jarRefetch(true);
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];

    const isDuplicate = () =>
      jars
        .filter(jar => jar.group.split('-')[0] === worker.name)
        .some(jar => jar.name === file.name);

    if (isDuplicate()) {
      showMessage('The plugin name already exists!');
      return;
    }

    if (event.target.files[0]) {
      uploadJar(file);
    }
  };

  const headRows = [
    { id: 'name', label: 'File name' },
    { id: 'size', label: 'File size(KB)' },
    { id: 'lastModified', label: 'Last modified' },
    { id: 'action', label: 'Action', sortable: false },
  ];

  const handleModalOpen = jarName => {
    setJarNameToBeDeleted(jarName);
    setIsModalOpen(true);
  };

  const handleUndo = workerName => {
    const newAction = workerActions.map(workerAction => {
      if (workerAction.name === workerName) {
        const newType = workerAction.type === 'READY' ? 'DELETE' : 'READY';
        const undoAction = {
          name: workerAction.name,
          size: workerAction.size,
          lastModified: workerAction.lastModified,
          type: newType,
          group: workerAction.group,
        };
        return undoAction;
      } else {
        return workerAction;
      }
    });

    setWorkerActions(newAction);
  };

  const workerActionButton = data => {
    const { name } = data;
    return (
      <>
        {get(
          workerActions.filter(workerAction => workerAction.name === name)[0],
          'type',
          'READY',
        ) === 'READY' ? (
          <Tooltip
            title={`Delete ${name} form this workerspace`}
            enterDelay={1000}
          >
            <IconButton data-testid={name} onClick={() => handleUndo(name)}>
              <ActionIcon className="fas fa-trash-alt" />
            </IconButton>
          </Tooltip>
        ) : (
          <Tooltip title={`Undo delete ${name}`} enterDelay={1000}>
            <IconButton data-testid={name} onClick={() => handleUndo(name)}>
              <ActionIcon className="fas fa-undo" />
            </IconButton>
          </Tooltip>
        )}
      </>
    );
  };

  const jarActionButton = data => {
    const { name } = data;
    return (
      <Tooltip title={`Delete ${name} form file`} enterDelay={1000}>
        <IconButton data-testid={name} onClick={() => handleModalOpen(name)}>
          <ActionIcon className="fas fa-trash-alt" />
        </IconButton>
      </Tooltip>
    );
  };

  const workerRows = workerActions.map(jar => {
    return {
      name: jar.name,
      size: floor(divide(jar.size, 1024), 1),
      lastModified: utils.getDateFromTimestamp(jar.lastModified),
      action: workerActionButton(jar),
      type: jar.type,
    };
  });

  const jarRows = jars
    .filter(jar => jar.group.split('-')[0] === worker.name)
    .filter(
      jar =>
        !worker.jarInfos.map(workerJars => workerJars.name).includes(jar.name),
    )
    .map(jar => {
      return {
        name: jar.name,
        size: floor(divide(jar.size, 1024), 1),
        lastModified: utils.getDateFromTimestamp(jar.lastModified),
        action: jarActionButton(jar),
        type: 'ADD',
      };
    });

  const handleDelete = async () => {
    if (jarNameToBeDeleted) {
      await deleteApi(`${jarNameToBeDeleted}?group=${worker.name}-plugin`);
      const isSuccess = get(getDeleteRes(), 'data.isSuccess', false);
      setDeleting(false);

      if (isSuccess) {
        showMessage(MESSAGES.STREAM_APP_DELETE_SUCCESS);
        setIsModalOpen(false);
        setJarNameToBeDeleted('');
        jarRefetch(true);
      }
    }
  };

  const resetWorkerAction = () => {
    const newAction = workerActions.map(workerAction => {
      if (workerAction.type === 'DELETE') {
        const undoAction = {
          name: workerAction.name,
          size: workerAction.size,
          lastModified: workerAction.lastModified,
          type: 'READY',
          group: workerAction.group,
        };
        return undoAction;
      } else {
        return workerAction;
      }
    });

    setWorkerActions(newAction);
  };

  const handleDiscard = async () => {
    resetWorkerAction();

    const newJar = jars
      .filter(jar => jar.group.split('-')[0] === worker.name)
      .filter(
        jar =>
          !worker.jarInfos
            .map(workerJars => workerJars.name)
            .includes(jar.name),
      );

    newJar.forEach(jar => deleteApi(`${jar.name}?group=${worker.name}-plugin`));

    await commonUtils.sleep(500);

    jarRefetch(true);
  };

  const handleRestart = async () => {
    setIsLoading(true);

    await getPipeline();
    const res = get(pipelineRes(), 'data.result', []);

    //Get the connector in execution
    let runningConnector = [];
    res
      .filter(pipeline => pipeline.tags.workerClusterName === worker.name)
      .map(pipeline => pipeline.objects)
      .flatMap(object => object)
      .filter(
        object =>
          object.state === 'RUNNING' &&
          object.kind !== 'stream' &&
          object.kind !== 'topic',
      )
      .forEach(connector => {
        runningConnector.push(connector);
        putConnector(`/${connector.name}/stop?group=${connector.group}`);
      });

    const stopConnectorCheckFn = res => {
      return isUndefined(get(res, 'data.result.state', undefined));
    };

    const startConnectorCheckFn = res => {
      return get(res, 'data.result.state', undefined) === 'RUNNING';
    };

    //Stop all connectors
    /* eslint-disable no-unused-vars */
    for (let stopConnector of runningConnector) {
      const stopConnectorParams = {
        url: `${URL.CONNECTOR_URL}/${stopConnector.name}?group=${stopConnector.group}`,
        checkFn: stopConnectorCheckFn,
        sleep: 3000,
      };

      await waitApi(stopConnectorParams);
    }

    setActiveStep(1);

    const stopWorkerCheckFn = res => {
      return isUndefined(get(res, 'data.result.state', undefined));
    };

    const startWorkerCheckFn = res => {
      return get(res, 'data.result.connectors', []).length > 0;
    };

    await putWorker(`/${worker.name}/stop`);

    const stopWorkerParams = {
      url: `${URL.WORKER_URL}/${worker.name}`,
      checkFn: stopWorkerCheckFn,
      sleep: 3000,
    };

    const startWorkerParams = {
      url: `${URL.WORKER_URL}/${worker.name}`,
      checkFn: startWorkerCheckFn,
      sleep: 3000,
    };

    await waitApi(stopWorkerParams);

    setActiveStep(2);

    let removeWorkerPlugin = [];
    let reserveWorkerPlugin = [];

    //Get files that users want to delete in the workspace
    const reservePlugin = workerActions
      .map(workerAction => {
        if (workerAction.type === 'DELETE') {
          removeWorkerPlugin.push(workerAction);
        } else {
          reserveWorkerPlugin.push(workerAction);
        }
        return workerAction;
      })
      .filter(workerAction => workerAction.type !== 'DELETE')
      .map(workerAction => {
        return {
          name: workerAction.name,
          group: workerAction.group,
        };
      });

    //Get files that users want to add in the workspace
    const addPlugin = jars
      .filter(jar => jar.group.split('-')[0] === worker.name)
      .filter(
        jar =>
          !worker.jarInfos
            .map(workerJars => workerJars.name)
            .includes(jar.name),
      )
      .map(jar => {
        return {
          name: jar.name,
          group: jar.group,
        };
      });

    //Update workspace
    await putWorker(`/${worker.name}`, {
      jarKeys: reservePlugin.concat(addPlugin),
    });

    await putWorker(`/${worker.name}/start`);

    await waitApi(startWorkerParams);

    setActiveStep(3);

    //Remove files that users want to discard
    removeWorkerPlugin.forEach(jar =>
      deleteApi(`${jar.name}?group=${jar.group}`),
    );

    //Waiting to loading bars and delete files
    await commonUtils.sleep(2500);

    runningConnector.forEach(connector => {
      putConnector(`/${connector.name}/start?group=${connector.group}`);
    });
    /* eslint-disable no-unused-vars */
    for (let startConnector of runningConnector) {
      const startConnectorParams = {
        url: `${URL.CONNECTOR_URL}/${startConnector.name}?group=${startConnector.group}`,
        checkFn: startConnectorCheckFn,
        sleep: 3000,
      };

      await waitApi(startConnectorParams);
    }

    setWorkerActions([]);

    workerRefetch(true);

    jarRefetch(true);

    setActiveStep(4);
  };

  const steps = [
    'StopConnector',
    'StopWorkspace',
    'UpdateWorkspace',
    'StartConnector',
  ];

  return (
    <>
      <StyledInputFile
        id="fileInput"
        accept=".jar"
        type="file"
        onChange={handleFileSelect}
        onClick={event => {
          /* Allow file to be added multiple times */
          event.target.value = null;
        }}
      />
      <StyledLabel htmlFor="fileInput">
        <Button component="span" text="ADD PLUGIN" />
      </StyledLabel>
      <Main>
        <SortTable
          isLoading={jarIsLoading}
          headRows={headRows}
          rows={workerRows.concat(jarRows)}
          tableName="plugins"
          handleDiscard={() => handleDiscard()}
          handleRestart={() => handleRestart()}
        />
      </Main>

      <DeleteDialog
        title="Delete File?"
        content={`Are you sure you want to delete the file: ${jarNameToBeDeleted} ? This action cannot be undone!`}
        open={isModalOpen}
        handleClose={() => setIsModalOpen(false)}
        handleConfirm={handleDelete}
        working={deleting}
      />
      <Progress
        open={isLoading}
        steps={steps}
        activeStep={activeStep}
        maxWidth="md"
        createTitle="Update Workspace..."
        deleteTitle="Failed to update workspace and revert to previous version..."
      />
    </>
  );
};

Plugins.propTypes = {
  worker: PropTypes.shape({
    name: PropTypes.string.isRequired,
    jarInfos: PropTypes.array.isRequired,
  }).isRequired,
  workerRefetch: PropTypes.func.isRequired,
};

export default Plugins;
