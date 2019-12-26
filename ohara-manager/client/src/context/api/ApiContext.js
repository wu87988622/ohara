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

import React, { createContext, useContext, useEffect, useState } from 'react';
import PropTypes from 'prop-types';

import { useSnackbar } from 'context/SnackbarContext';
import { useApp } from 'context';

import { createApi as createBrokerApi } from './brokerApi';
import { createApi as createFileApi } from './fileApi';
import { createApi as createWorkerApi } from './workerApi';
import { createApi as createWorkspaceApi } from './workspaceApi';
import { createApi as createZookeeperApi } from './zookeeperApi';

const ApiContext = createContext();

const ApiProvider = ({ children }) => {
  const { workspaceName, pipelineName } = useApp();
  const showMessage = useSnackbar();

  const [brokerApi, setBrokerApi] = useState();
  const [connectorApi, setConnectorApi] = useState();
  const [fileApi, setFileApi] = useState();
  const [logApi, setLogApi] = useState();
  const [nodeApi, setNodeApi] = useState();
  const [pipelineApi, setPipelineApi] = useState();
  const [streamApi, setStreamApi] = useState();
  const [topicApi, setTopicApi] = useState();
  const [workerApi, setWorkerApi] = useState();
  const [workspaceApi, setWorkspaceApi] = useState();
  const [zookeeperApi, setZookeeperApi] = useState();

  useEffect(() => {
    const context = {
      workspaceName,
      pipelineName,
      showMessage,
    };
    setBrokerApi(createBrokerApi(context));
    setConnectorApi();
    setFileApi(createFileApi(context));
    setLogApi();
    setNodeApi();
    setPipelineApi();
    setStreamApi();
    setTopicApi();
    setWorkerApi(createWorkerApi(context));
    setWorkspaceApi(createWorkspaceApi(context));
    setZookeeperApi(createZookeeperApi(context));
  }, [workspaceName, pipelineName, showMessage]);

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
