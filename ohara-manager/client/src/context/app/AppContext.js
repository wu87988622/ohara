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

import React, { createContext, useContext, useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import { hashByGroupAndName } from 'utils/sha';

const AppContext = createContext();

const AppProvider = ({ children }) => {
  const [workspaceName, setWorkspaceName] = useState(null);
  const [pipelineName, setPipelineName] = useState(null);

  const workspaceGroup = 'workspace';
  const zookeeperGroup = 'zookeeper';
  const brokerGroup = 'broker';
  const workerGroup = 'worker';
  const [connectorGroup, setConnectorGroup] = useState(null);
  const [fileGroup, setFileGroup] = useState(null);
  const [pipelineGroup, setPipelineGroup] = useState(null);
  const [streamGroup, setStreamGroup] = useState(null);
  const [topicGroup, setTopicGroup] = useState(null);

  const [brokerKey, setBrokerKey] = useState(null);
  const [workerKey, setWorkerKey] = useState(null);
  const [workspaceKey, setWorkspaceKey] = useState(null);
  const [zookeeperKey, setZookeeperKey] = useState(null);

  useEffect(() => {
    setBrokerKey(workspaceName && { group: brokerGroup, name: workspaceName });
    setWorkerKey(workspaceName && { group: workerGroup, name: workspaceName });
    setWorkspaceKey(
      workspaceName && { group: workspaceGroup, name: workspaceName },
    );
    setZookeeperKey(
      workspaceName && { group: zookeeperGroup, name: workspaceName },
    );

    const group =
      workspaceName && hashByGroupAndName(workspaceGroup, workspaceName);
    setFileGroup(group);
    setPipelineGroup(group);
    setTopicGroup(group);
  }, [workspaceName]);

  useEffect(() => {
    const group =
      pipelineGroup &&
      pipelineName &&
      hashByGroupAndName(pipelineGroup, pipelineName);
    setConnectorGroup(group);
    setStreamGroup(group);
  }, [pipelineGroup, pipelineName]);

  return (
    <AppContext.Provider
      value={{
        // name
        workspaceName,
        setWorkspaceName,
        pipelineName,
        setPipelineName,
        // group
        brokerGroup,
        connectorGroup,
        fileGroup,
        pipelineGroup,
        streamGroup,
        topicGroup,
        workerGroup,
        workspaceGroup,
        zookeeperGroup,
        // key
        brokerKey,
        workerKey,
        workspaceKey,
        zookeeperKey,
      }}
    >
      {children}
    </AppContext.Provider>
  );
};

const useApp = () => {
  const context = useContext(AppContext);
  if (context === undefined) {
    throw new Error('useApp must be used within a AppProvider');
  }

  return context;
};

AppProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { AppProvider, useApp };
