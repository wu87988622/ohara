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

import { isEmpty, map } from 'lodash';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import * as zookeeperApi from 'api/zookeeperApi';
import { generateClusterResponse, validate } from './utils';
import { ZOOKEEPER } from './index';

export const createApi = context => {
  const { showMessage } = context;
  const group = ZOOKEEPER;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await zookeeperApi.getAll(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return await Promise.all(
        map(res.data, async zookeeper => {
          const params = { name: zookeeper.name, group };
          const stageRes = await objectApi.get(params);
          if (!isEmpty(stageRes.errors)) {
            throw new Error(stageRes.title);
          }
          const infoRes = await inspectApi.getZookeeperInfo(params);
          if (!isEmpty(infoRes.errors)) {
            throw new Error(infoRes.title);
          }
          return generateClusterResponse({
            values: zookeeper,
            stageValues: stageRes.data,
            inspectInfo: infoRes.data,
          });
        }),
      );
    },
    fetch: async name => {
      const params = { name, group };
      const res = await zookeeperApi.get(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      const stageRes = await objectApi.get(params);
      if (!isEmpty(stageRes.errors)) {
        throw new Error(stageRes.title);
      }
      const infoRes = await inspectApi.getZookeeperInfo(params);
      if (!isEmpty(infoRes.errors)) {
        throw new Error(infoRes.title);
      }
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: infoRes.data,
      });
    },
    create: async values => {
      try {
        validate(values);
        const params = { ...values, group };
        const res = await zookeeperApi.create(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const stageRes = await objectApi.create(params);
        if (!isEmpty(stageRes.errors)) {
          throw new Error(stageRes.title);
        }
        const infoRes = await inspectApi.getZookeeperInfo(params);
        if (!isEmpty(infoRes.errors)) {
          throw new Error(infoRes.title);
        }
        const data = generateClusterResponse({
          values: res.data,
          stageValues: stageRes.data,
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
        const params = { ...values, group };
        const res = await zookeeperApi.update(params);
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
    stage: async values => {
      try {
        validate(values);
        const params = { ...values, group };
        const stageRes = await objectApi.update(params);
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Save zookeeper ${values.name} failed.`);
        }
        const data = generateClusterResponse({ stageValues: stageRes.data });
        showMessage(`Save zookeeper ${values.name} successful.`);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const params = { name, group };
        const res = await zookeeperApi.remove(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const stageRes = await objectApi.remove(params);
        if (!isEmpty(stageRes.errors)) {
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
        const res = await zookeeperApi.start(params);
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
        const res = await zookeeperApi.stop(params);
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
