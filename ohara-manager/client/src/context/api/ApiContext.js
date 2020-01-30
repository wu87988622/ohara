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

import React, { createContext, useContext, useMemo } from 'react';
import PropTypes from 'prop-types';

import { useSnackbar } from 'context/SnackbarContext';
import { useApp, useEventLogActions } from 'context';
import { createApi as createBrokerApi } from './brokerApi';
import { createApi as createFileApi } from './fileApi';
import { createApi as createNodeApi } from './nodeApi';
import { createApi as createTopicApi } from './topicApi';
import { createApi as createWorkerApi } from './workerApi';
import { createApi as createConnectorApi } from './connectorApi';
import { createApi as createWorkspaceApi } from './workspaceApi';
import { createApi as createZookeeperApi } from './zookeeperApi';
import { createApi as createLogApi } from './logApi';
import { createApi as createPipelineApi } from './pipelineApi';
import { createApi as createStreamApi } from './streamApi';

const ApiContext = createContext();

const ApiProvider = ({ children }) => {
  const {
    brokerGroup,
    connectorGroup,
    fileGroup,
    pipelineGroup,
    streamGroup,
    topicGroup,
    workerGroup,
    workspaceGroup,
    zookeeperGroup,
    brokerKey,
    workerKey,
    workspaceKey,
  } = useApp();
  const showMessage = useSnackbar();
  const { createEventLog } = useEventLogActions();

  const brokerApi = useMemo(
    () => createBrokerApi({ brokerGroup, zookeeperGroup, showMessage }),
    [brokerGroup, zookeeperGroup, showMessage],
  );

  const connectorApi = useMemo(
    () =>
      createConnectorApi({
        connectorGroup,
        workerKey,
        showMessage,
        topicGroup,
      }),
    [connectorGroup, workerKey, showMessage, topicGroup],
  );

  const fileApi = useMemo(
    () => createFileApi({ fileGroup, workspaceKey, showMessage }),
    [fileGroup, workspaceKey, showMessage],
  );

  const logApi = useMemo(
    () =>
      createLogApi({
        workspaceKey,
        brokerGroup,
        streamGroup,
        workerGroup,
        zookeeperGroup,
      }),
    [workspaceKey, brokerGroup, streamGroup, workerGroup, zookeeperGroup],
  );

  const nodeApi = useMemo(() => createNodeApi({ showMessage }), [showMessage]);

  const pipelineApi = useMemo(
    () => createPipelineApi({ pipelineGroup, showMessage, createEventLog }),
    [pipelineGroup, showMessage, createEventLog],
  );

  const streamApi = useMemo(
    () => createStreamApi({ streamGroup, brokerKey, showMessage, topicGroup }),
    [streamGroup, brokerKey, showMessage, topicGroup],
  );

  const topicApi = useMemo(
    () => createTopicApi({ topicGroup, brokerKey, workspaceKey, showMessage }),
    [topicGroup, brokerKey, workspaceKey, showMessage],
  );

  const workerApi = useMemo(
    () => createWorkerApi({ workerGroup, brokerGroup, showMessage }),
    [workerGroup, brokerGroup, showMessage],
  );

  const workspaceApi = useMemo(
    () => createWorkspaceApi({ workspaceGroup, showMessage }),
    [workspaceGroup, showMessage],
  );

  const zookeeperApi = useMemo(
    () => createZookeeperApi({ zookeeperGroup, showMessage }),
    [zookeeperGroup, showMessage],
  );

  return (
    <ApiContext.Provider
      value={{
        brokerApi,
        connectorApi,
        fileApi,
        logApi,
        nodeApi,
        pipelineApi,
        streamApi,
        topicApi,
        workerApi,
        workspaceApi,
        zookeeperApi,
      }}
    >
      {children}
    </ApiContext.Provider>
  );
};

const useApi = () => {
  const context = useContext(ApiContext);
  if (context === undefined) {
    throw new Error('useApi must be used within a ApiProvider');
  }

  return context;
};

ApiProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { ApiProvider, useApi };
