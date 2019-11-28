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
import { isEmpty, get } from 'lodash';

import { useWorkerState, useBrokerState, useZookeeperState } from 'context';

const WorkspaceContext = createContext();

const WorkspaceProvider = ({ children }) => {
  const [workspaceName, setWorkspaceName] = useState(null);
  const [currentWorker, setCurrentWorker] = useState(null);
  const [currentBroker, setCurrentBroker] = useState(null);
  const [currentZookeeper, setCurrentZookeeper] = useState(null);
  const { data: workers, isFetching } = useWorkerState();
  const { data: brokers } = useBrokerState();
  const { data: zookeepers } = useZookeeperState();

  // Set the current worker
  useEffect(() => {
    if (isEmpty(workers) || !workspaceName) return;
    if (get(currentWorker, 'settings.name') === workspaceName) return;
    const workerFound = workers.find(
      worker => worker.settings.name === workspaceName,
    );
    setCurrentWorker(workerFound);
  }, [workers, currentWorker, workspaceName]);

  // Set the current broker
  useEffect(() => {
    if (isEmpty(brokers) || isEmpty(currentWorker)) return;
    if (
      get(currentBroker, 'settings.name') ===
      get(currentWorker, 'settings.brokerClusterKey.name')
    ) {
      return;
    }
    const brokerFound = brokers.find(
      broker =>
        broker.settings.name ===
        get(currentWorker, 'settings.brokerClusterKey.name'),
    );
    setCurrentBroker(brokerFound);
  }, [brokers, currentBroker, currentWorker]);

  // Set the current zookeeper
  useEffect(() => {
    if (isEmpty(zookeepers) || isEmpty(currentBroker)) return;
    if (
      get(currentZookeeper, 'settings.name') ===
      get(currentBroker, 'settings.zookeeperClusterKey.name')
    ) {
      return;
    }
    const zookeeperFound = zookeepers.find(
      zookeeper =>
        zookeeper.settings.name ===
        get(currentBroker, 'settings.zookeeperClusterKey.name'),
    );
    setCurrentZookeeper(zookeeperFound);
  }, [zookeepers, currentZookeeper, currentBroker]);

  const findByWorkspaceName = workspaceName => {
    const workspaces = workers;
    return workspaces.find(
      workspace => workspace.settings.name === workspaceName,
    );
  };

  return (
    <WorkspaceContext.Provider
      value={{
        workspaces: workers,
        currentWorkspace: currentWorker,
        currentWorker,
        currentBroker,
        currentZookeeper,
        isFetching,
        workspaceName,
        setWorkspaceName,
        findByWorkspaceName,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
};

const useWorkspace = () => {
  const context = useContext(WorkspaceContext);

  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }

  return context;
};

WorkspaceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { WorkspaceProvider, useWorkspace };
