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

import { filter, find, map } from 'lodash';
import { SERVICE_NAME } from 'const';

export const transformNode = (node, clusters) => {
  const { brokers, workers, streams, zookeepers } = clusters;
  return {
    ...node,
    services: map(node?.services, service => {
      return {
        ...service,
        clusters: filter(
          map(service.clusterKeys, clusterKey => {
            switch (service.name) {
              case SERVICE_NAME.BROKER:
                return find(brokers, b => b.name === clusterKey.name);
              case SERVICE_NAME.CONFIGURATOR:
                return null;
              case SERVICE_NAME.WORKER:
                return find(workers, w => w.name === clusterKey.name);
              case SERVICE_NAME.STREAM:
                return find(streams, s => s.name === clusterKey.name);
              case SERVICE_NAME.ZOOKEEPER:
                return find(zookeepers, z => z.name === clusterKey.name);
              default:
                throw Error('Unknown service name: ', service.name);
            }
          }),
        ),
      };
    }),
  };
};

export const transformNodes = (nodes, clusters) => {
  return map(nodes, node => transformNode(node, clusters));
};
