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

import { useApp } from 'context';
import { createApi as createTopicApi } from './topicApi';
import { createApi as createConnectorApi } from './connectorApi';
import { createApi as createLogApi } from './logApi';
import { createApi as createPipelineApi } from './pipelineApi';

const ApiContext = createContext();

const ApiProvider = ({ children }) => {
  const {
    brokerGroup,
    connectorGroup,
    pipelineGroup,
    streamGroup,
    topicGroup,
    workerGroup,
    zookeeperGroup,
    brokerKey,
    workerKey,
    workspaceKey,
  } = useApp();

  const connectorApi = useMemo(
    () => createConnectorApi({ connectorGroup, workerKey, topicGroup }),
    [connectorGroup, workerKey, topicGroup],
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

  const pipelineApi = useMemo(() => createPipelineApi({ pipelineGroup }), [
    pipelineGroup,
  ]);

  const topicApi = useMemo(
    () => createTopicApi({ topicGroup, brokerKey, workspaceKey }),
    [topicGroup, brokerKey, workspaceKey],
  );

  return (
    <ApiContext.Provider
      value={{
        connectorApi,
        logApi,
        pipelineApi,
        topicApi,
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
