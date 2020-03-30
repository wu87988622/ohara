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
import { get, size, xor, merge, filter, includes } from 'lodash';

import * as hooks from 'hooks';

const EditWorkspaceContext = createContext();

const EditWorkspaceProvider = ({ children }) => {
  const currBk = hooks.useBroker();
  const currWk = hooks.useWorker();
  const currZk = hooks.useZookeeper();
  const allNodesInOhara = hooks.useAllNodes();

  const [stagingNodes, setStagingNodes] = useState([]);
  const [dirties, setDirties] = useState(null);

  const calcDirties = cluster => {
    const settings = get(cluster, 'settings');
    const stagingSettings = get(cluster, 'stagingSettings');

    const nodes = get(settings, 'nodeNames');
    const stagingNodes = get(stagingSettings, 'nodeNames');

    return {
      nodes: size(xor(nodes, stagingNodes)),
    };
  };

  useEffect(() => {
    const wkDirties = calcDirties(currWk);
    const bkDirties = calcDirties(currBk);
    const zkDirties = calcDirties(currZk);

    const worker = 0;
    const broker = 0;
    const zookeeper = 0;
    const plugins = 0;
    const nodes =
      get(wkDirties, 'nodes') +
      get(bkDirties, 'nodes') +
      get(zkDirties, 'nodes');

    setDirties({
      all: worker + broker + zookeeper + plugins + nodes,
      settings: {
        worker,
        broker,
        zookeeper,
      },
      plugins,
      nodes,
    });
  }, [currWk, currBk, currZk]);

  useEffect(() => {
    if (!currWk || !currBk || !currZk || !allNodesInOhara) return;
    const usedNodeNames = merge(
      get(currWk, 'nodeNames'),
      get(currBk, 'nodeNames'),
      get(currZk, 'nodeNames'),
      get(currWk, 'stagingSettings.nodeNames'),
      get(currBk, 'stagingSettings.nodeNames'),
      get(currZk, 'stagingSettings.nodeNames'),
    );
    const stagingNodes = filter(allNodesInOhara, node =>
      includes(usedNodeNames, get(node, 'hostname')),
    );
    setStagingNodes(stagingNodes);
  }, [currWk, currBk, currZk, allNodesInOhara]);

  return (
    <EditWorkspaceContext.Provider
      value={{
        dirties,
        stagingNodes,
      }}
    >
      {children}
    </EditWorkspaceContext.Provider>
  );
};

const useEditWorkspace = () => {
  const context = useContext(EditWorkspaceContext);

  if (context === undefined) {
    throw new Error(
      'useEditWorkspace must be used within a EditWorkspaceProvider',
    );
  }

  return context;
};

EditWorkspaceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { EditWorkspaceProvider, useEditWorkspace };
