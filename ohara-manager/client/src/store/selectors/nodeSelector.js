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

import { filter, map, sortBy, values } from 'lodash';
import { createSelector } from 'reselect';
import * as transforms from 'store/transforms';

const sort = nodes => sortBy(nodes, ['hostname']);

const getBrokerEntities = state => state?.entities?.brokers;

const getNodeEntities = state => state?.entities?.nodes;

const getStreamEntities = state => state?.entities?.streams;

const getWorkerEntities = state => state?.entities?.workers;

const getZookeeperEntities = state => state?.entities?.zookeepers;

const getNamesFormProps = (_, props) => props?.names;

const getClusters = createSelector(
  [
    getBrokerEntities,
    getStreamEntities,
    getWorkerEntities,
    getZookeeperEntities,
  ],
  (brokerEntities, streamEntities, workerEntities, zookeeperEntities) => ({
    brokers: values(brokerEntities),
    streams: values(streamEntities),
    workers: values(workerEntities),
    zookeepers: values(zookeeperEntities),
  }),
);

export const getAllNodes = createSelector(
  [getNodeEntities, getClusters],
  (nodeEntities, clusters) => {
    const sortedNodes = sort(values(nodeEntities));
    return transforms.transformNodes(sortedNodes, clusters);
  },
);

export const makeGetNodesByNames = () =>
  createSelector(
    [getNodeEntities, getClusters, getNamesFormProps],
    (nodeEntities, clusters, names) => {
      const sortedNodes = sort(filter(map(names, name => nodeEntities[name])));
      return transforms.transformNodes(sortedNodes, clusters);
    },
  );
