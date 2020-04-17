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

import { filter, get, map } from 'lodash';
import { getId } from 'utils/object';
import { SERVICE_NAME } from 'const';
import { ENTITY_TYPE } from 'store/schema';

const convertToEntityType = serviceName => {
  switch (serviceName) {
    case SERVICE_NAME.BROKER:
      return ENTITY_TYPE.brokers;
    case SERVICE_NAME.CONFIGURATOR:
      return null;
    case SERVICE_NAME.WORKER:
      return ENTITY_TYPE.workers;
    case SERVICE_NAME.STREAM:
      return ENTITY_TYPE.streams;
    case SERVICE_NAME.ZOOKEEPER:
      return ENTITY_TYPE.zookeepers;
    default:
      throw Error('Unknown service name: ', serviceName);
  }
};

export const transformNode = (node, allEntities) => {
  return {
    ...node,
    services: map(node?.services, service => {
      return {
        ...service,
        clusters: filter(
          map(service.clusterKeys, clusterKey => {
            const entityType = convertToEntityType(service.name);
            if (entityType) {
              return get(allEntities, [entityType, getId(clusterKey)]);
            }
          }),
        ),
      };
    }),
  };
};

export const transformNodes = (nodes, allEntities) => {
  return map(nodes, node => transformNode(node, allEntities));
};
