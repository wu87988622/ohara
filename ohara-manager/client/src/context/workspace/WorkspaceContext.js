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
import { isEmpty, isEqual } from 'lodash';

import * as context from 'context';
import { usePrevious } from 'utils/hooks';

const WorkspaceContext = React.createContext();

const WorkspaceProvider = ({ children }) => {
  const { workspaceName } = context.useApp();
  const { data: workers } = context.useWorkerState();
  const { data: brokers } = context.useBrokerState();
  const { data: zookeepers } = context.useZookeeperState();

  /**
   * Abbreviations:
   * worker => wk
   * broker => bk
   * zookeeper => zk
   */
  const [currWk, setCurrWk] = React.useState(null);
  const [currBk, setCurrBk] = React.useState(null);
  const [currZk, setCurrZk] = React.useState(null);
  const prevWk = usePrevious(currWk);
  const prevBk = usePrevious(currBk);
  const prevZk = usePrevious(currZk);

  // Set the current worker
  React.useEffect(() => {
    if (!workspaceName || isEmpty(workers)) return;
    const found = workers.find(wk => wk.name === workspaceName);
    if (!isEqual(found, prevWk)) setCurrWk(found);
  }, [workspaceName, workers, prevWk]);

  // Set the current broker
  React.useEffect(() => {
    if (!workspaceName || isEmpty(brokers)) return;
    const found = brokers.find(bk => bk.name === workspaceName);
    if (!isEqual(found, prevBk)) setCurrBk(found);
  }, [workspaceName, brokers, prevBk]);

  // Set the current zookeeper
  React.useEffect(() => {
    if (!workspaceName || isEmpty(zookeepers)) return;
    const found = zookeepers.find(zk => zk.name === workspaceName);
    if (!isEqual(found, prevZk)) setCurrZk(found);
  }, [workspaceName, zookeepers, prevZk]);

  return (
    <WorkspaceContext.Provider
      value={{
        currentWorker: currWk,
        currentBroker: currBk,
        currentZookeeper: currZk,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
};

const useWorkspace = () => {
  const context = React.useContext(WorkspaceContext);
  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }
  return context;
};

WorkspaceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { WorkspaceProvider, useWorkspace };
