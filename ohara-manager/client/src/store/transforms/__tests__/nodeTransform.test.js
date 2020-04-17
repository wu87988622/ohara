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

import { GROUP, SERVICE_NAME } from 'const';
import { generateState } from 'utils/test';
import { transformNode, transformNodes } from '../nodeTransform';

test('transform node', () => {
  const testData = generateState({
    numberOfNodes: 1,
    numberOfWorkspaces: 1,
  });

  const expected = {
    hostname: 'node1',
    services: [
      {
        name: SERVICE_NAME.BROKER,
        clusterKeys: [{ group: GROUP.BROKER, name: 'workspace1' }],
        clusters: [
          { group: GROUP.BROKER, name: 'workspace1', state: 'RUNNING' },
        ],
      },
      {
        name: SERVICE_NAME.WORKER,
        clusterKeys: [{ group: GROUP.WORKER, name: 'workspace1' }],
        clusters: [
          { group: GROUP.WORKER, name: 'workspace1', state: 'RUNNING' },
        ],
      },
      {
        name: SERVICE_NAME.ZOOKEEPER,
        clusterKeys: [{ group: GROUP.ZOOKEEPER, name: 'workspace1' }],
        clusters: [
          { group: GROUP.ZOOKEEPER, name: 'workspace1', state: 'RUNNING' },
        ],
      },
    ],
  };
  const node = testData.entities.nodes.node1;
  const allEntities = testData.entities;
  const received = transformNode(node, allEntities);
  expect(received).toMatchObject(expected);
});

test('nodeTransform.transformNodes', () => {
  const testData = generateState({
    numberOfNodes: 2,
    numberOfWorkspaces: 2,
  });

  const nodes = [testData.entities.nodes.node1, testData.entities.nodes.node2];
  const allEntities = testData.entities;
  const received = transformNodes(nodes, allEntities);
  expect(received?.length).toBe(2);
});
