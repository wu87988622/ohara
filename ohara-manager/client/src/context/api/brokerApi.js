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

import { map } from 'lodash';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import * as brokerApi from 'api/brokerApi';
import { getKey } from 'utils/object';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { brokerGroup: group, zookeeperGroup, showMessage } = context;

  return {
    fetchAll: async () => {
      const params = { group };
      const res = await brokerApi.getAll(params);
      if (res.errors) throw new Error(res.title);

      return await Promise.all(
        map(res.data, async broker => {
          const params = { name: broker.name, group };
          const stageRes = await objectApi.get(params);
          if (stageRes.errors) throw new Error(stageRes.title);

          const infoRes = await inspectApi.getBrokerInfo(params);
          if (infoRes.errors) throw new Error(infoRes.title);

          return generateClusterResponse({
            values: broker,
            stageValues: stageRes.data,
            inspectInfo: infoRes.data,
          });
        }),
      );
    },
    fetch: async name => {
      const params = { name, group };
      const res = await brokerApi.get(params);
      if (res.errors) throw new Error(res.title);

      const stageRes = await objectApi.get(params);
      if (stageRes.errors) throw new Error(stageRes.title);

      const infoRes = await inspectApi.getBrokerInfo(params);
      if (infoRes.errors) throw new Error(infoRes.title);

      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: infoRes.data,
      });
    },
    create: async values => {
      try {
        validate(values);
        const zookeeperClusterKey = {
          group: zookeeperGroup,
          name: values.name,
        };
        const ensuredValues = { ...values, group, zookeeperClusterKey };
        const res = await brokerApi.create(ensuredValues);
        if (res.errors) throw new Error(res.title);

        const stageRes = await objectApi.create(res.data);
        if (stageRes.errors) throw new Error(stageRes.title);

        const infoRes = await inspectApi.getBrokerInfo(ensuredValues);
        if (infoRes.errors) throw new Error(infoRes.title);

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
        const ensuredValues = { ...values, group };
        const res = await brokerApi.update(ensuredValues);
        if (res.errors) throw new Error(res.title);

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
        const ensuredValues = { ...values, group };
        const stageRes = await objectApi.update(ensuredValues);
        if (stageRes.errors)
          throw new Error(`Save broker ${values.name} failed.`);

        const data = generateClusterResponse({ stageValues: stageRes.data });
        const key = getKey(stageRes.data);
        showMessage(`Save broker ${values.name} successful.`);
        return { ...data, ...key };
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const params = { name, group };
        const res = await brokerApi.remove(params);
        if (res.errors) throw new Error(res.title);

        const stageRes = await objectApi.remove(params);
        if (stageRes.errors) throw new Error(res.title);

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
        const res = await brokerApi.start(params);
        if (res.errors) throw new Error(res.title);

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
        const res = await brokerApi.stop(params);
        if (res.errors) throw new Error(res.title);

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
