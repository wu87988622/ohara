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

import { keyBy, range } from 'lodash';
import { GROUP, ServiceName } from 'const';
import { getId } from 'utils/object';

export const generateState = (options) => {
  const numberOfNodes = options?.numberOfNodes || 1;
  const numberOfWorkspaces = options?.numberOfWorkspaces || 1;
  const prefixNodeName = options?.prefixNodeName || 'node';
  const prefixWorkspaceName = options?.prefixWorkspaceName || 'workspace';

  const brokers = range(numberOfWorkspaces).map((index) => {
    return {
      group: GROUP.BROKER,
      name: `${prefixWorkspaceName}${index + 1}`,
      state: 'RUNNING',
    };
  });

  const nodes = range(numberOfNodes).map((index) => {
    return {
      hostname: `${prefixNodeName}${index + 1}`,
      resources: [],
      services: [
        {
          name: ServiceName.BROKER,
          clusterKeys: range(numberOfWorkspaces).map((index) => {
            return {
              group: GROUP.BROKER,
              name: `${prefixWorkspaceName}${index + 1}`,
            };
          }),
        },
        {
          name: ServiceName.WORKER,
          clusterKeys: range(numberOfWorkspaces).map((index) => {
            return {
              group: GROUP.WORKER,
              name: `${prefixWorkspaceName}${index + 1}`,
            };
          }),
        },
        {
          name: ServiceName.ZOOKEEPER,
          clusterKeys: range(numberOfWorkspaces).map((index) => {
            return {
              group: GROUP.ZOOKEEPER,
              name: `${prefixWorkspaceName}${index + 1}`,
            };
          }),
        },
      ],
    };
  });

  const workers = range(numberOfWorkspaces).map((index) => {
    return {
      group: GROUP.WORKER,
      name: `${prefixWorkspaceName}${index + 1}`,
      state: 'RUNNING',
    };
  });

  const workspaces = range(numberOfWorkspaces).map((index) => {
    return {
      group: GROUP.WORKSPACE,
      name: `${prefixWorkspaceName}${index + 1}`,
    };
  });

  const zookeepers = range(numberOfWorkspaces).map((index) => {
    return {
      group: GROUP.ZOOKEEPER,
      name: `${prefixWorkspaceName}${index + 1}`,
      state: 'RUNNING',
    };
  });

  return {
    entities: {
      brokers: keyBy(brokers, getId),
      nodes: keyBy(nodes, 'hostname'),
      workers: keyBy(workers, getId),
      workspaces: keyBy(workspaces, getId),
      zookeepers: keyBy(zookeepers, getId),
    },
  };
};
