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

import { isEmpty, has, map } from 'lodash';

import * as inspectApi from 'api/inspectApi';
import * as streamApi from 'api/streamApi';
import { generateClusterResponse, validate } from './utils';

const validateJarKey = values => {
  if (!has(values, 'jarKey'))
    throw new Error('The values must contain the jarKey property');
};

export const createApi = context => {
  const { streamGroup, brokerKey, showMessage, topicGroup } = context;
  if (!streamGroup || !brokerKey) return;

  const group = streamGroup;
  const brokerClusterKey = brokerKey;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await streamApi.getAll(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return await Promise.all(
        map(res.data, async stream => {
          const infoRes = await inspectApi.getStreamsInfo(stream);
          if (!isEmpty(infoRes.errors)) {
            throw new Error(infoRes.title);
          }
          return generateClusterResponse({
            values: stream,
            inspectInfo: infoRes.data,
          });
        }),
      );
    },
    fetch: async name => {
      const params = { name, group };
      const res = await streamApi.get(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      const infoRes = await inspectApi.getStreamsInfo(params);
      if (!isEmpty(infoRes.errors)) {
        throw new Error(infoRes.title);
      }
      return generateClusterResponse({
        values: res.data,
        inspectInfo: infoRes.data,
      });
    },
    create: async values => {
      try {
        validate(values);
        validateJarKey(values);
        const params = {
          ...values,
          group,
          brokerClusterKey,
        };
        const res = await streamApi.create(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const infoRes = await inspectApi.getStreamsInfo(params);
        if (!isEmpty(infoRes.errors)) {
          throw new Error(infoRes.title);
        }
        const data = generateClusterResponse({
          values: res.data,
          inspectInfo: infoRes.data,
        });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    update: async values => {
      try {
        validate(values);
        if (!isEmpty(values.to)) {
          values.to = values.to.map(to => {
            return {
              name: to.name,
              group: topicGroup,
            };
          });
        }
        if (!isEmpty(values.from)) {
          values.from = values.from.map(from => {
            return {
              name: from.name,
              group: topicGroup,
            };
          });
        }
        const params = { ...values, group };
        const res = await streamApi.update(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const data = generateClusterResponse({ values: res.data });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const params = { name, group };
        const res = await streamApi.remove(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        showMessage(res.title);
        return params;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    start: async name => {
      try {
        const params = { name, group };
        const res = await streamApi.start(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const data = generateClusterResponse({ values: res.data });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    stop: async name => {
      try {
        const params = { name, group };
        const res = await streamApi.stop(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const data = generateClusterResponse({ values: res.data });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
  };
};
