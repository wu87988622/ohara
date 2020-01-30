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

import { has, isEmpty, map } from 'lodash';

import * as connectorApi from 'api/connectorApi';
import * as inspectApi from 'api/inspectApi';
import ContextApiError from 'context/ContextApiError';
import { generateClusterResponse } from './utils';

const validateName = values => {
  if (!has(values, 'name'))
    throw new ContextApiError({
      title: 'The request should contain name in payload',
    });
};

const validateClassName = values => {
  if (!has(values, 'connector__class'))
    throw new ContextApiError({
      title: 'The request should contain connector__class in payload',
    });
};

export const createApi = context => {
  const { connectorGroup, workerKey, topicGroup } = context;
  if (!connectorGroup || !workerKey) return;

  const group = connectorGroup;
  const workerClusterKey = workerKey;

  const getDefinition = async className => {
    const workerInfo = await inspectApi.getWorkerInfo(workerClusterKey);
    if (workerInfo.errors) throw new ContextApiError(workerInfo);
    const connectorDefinition = workerInfo.data.classInfos
      // the "connector__class" field is converted by "connector.class" from request
      // each connector creation must assign connector.class
      .find(param => param.className === className);
    if (!connectorDefinition)
      throw new ContextApiError({
        ...workerInfo,
        title: `Cannot find required definitions of ${className}.`,
      });
    return connectorDefinition;
  };

  return {
    create: async values => {
      validateName(values);
      validateClassName(values);
      const res = await connectorApi.create({
        ...values,
        group,
        workerClusterKey,
      });
      if (res.errors) throw new ContextApiError(res);
      const info = await getDefinition(values.connector__class);
      return generateClusterResponse({
        values: res.data,
        inspectInfo: info,
      });
    },
    update: async values => {
      validateName(values);
      if (!isEmpty(values.topicKeys)) {
        values.topicKeys = values.topicKeys.map(topicKey => {
          return {
            name: topicKey.name,
            group: topicGroup,
          };
        });
      }
      const res = await connectorApi.update({
        ...values,
        group,
      });
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    delete: async name => {
      const params = { name, group };
      const res = await connectorApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      return params;
    },
    fetch: async name => {
      const params = { name, group };
      const res = await connectorApi.get(params);
      if (res.errors) throw new ContextApiError(res);
      const info = await getDefinition(res.data.connector__class);
      return generateClusterResponse({
        values: res.data,
        inspectInfo: info,
      });
    },
    fetchAll: async () => {
      const res = await connectorApi.getAll({ group });
      if (res.errors) throw new ContextApiError(res);
      return await Promise.all(
        map(res.data, async connector => {
          const info = await getDefinition(connector.connector__class);
          return generateClusterResponse({
            values: connector,
            inspectInfo: info,
          });
        }),
      );
    },
    start: async name => {
      const params = { name, group };
      const res = await connectorApi.start(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    stop: async name => {
      const params = { name, group };
      const res = await connectorApi.stop(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
  };
};
