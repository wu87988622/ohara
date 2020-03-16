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
import ContextApiError from 'context/ContextApiError';
import { getKey } from 'utils/object';
import { generateClusterResponse, validate } from './utils';
import { API, RESOURCE } from 'api/utils/apiUtils';
import { wait, waitForRunning } from 'api/utils/waitUtils';

export const createApi = context => {
  const { brokerGroup, zookeeperGroup } = context;
  if (!brokerGroup || !zookeeperGroup) return;

  const group = brokerGroup;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await brokerApi.getAll(params);
      if (res.errors) throw new ContextApiError(res);
      return await Promise.all(
        map(res.data, async broker => {
          const params = { name: broker.name, group };
          const stageRes = await objectApi.get(params);
          if (stageRes.errors) throw new ContextApiError(stageRes);
          const infoRes = await inspectApi.getBrokerInfo(params);
          if (infoRes.errors) throw new ContextApiError(infoRes);
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
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.get(params);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      const infoRes = await inspectApi.getBrokerInfo(params);
      if (infoRes.errors) throw new ContextApiError(infoRes);
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: infoRes.data,
      });
    },
    create: async values => {
      validate(values);
      const zookeeperClusterKey = {
        group: zookeeperGroup,
        name: values.name,
      };
      const ensuredValues = { ...values, group, zookeeperClusterKey };
      const res = await brokerApi.create(ensuredValues);
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.create(res.data);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      const infoRes = await inspectApi.getBrokerInfo(ensuredValues);
      if (infoRes.errors) throw new ContextApiError(infoRes);
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: infoRes.data,
      });
    },
    update: async values => {
      validate(values);
      const ensuredValues = { ...values, group };
      const res = await brokerApi.update(ensuredValues);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    stage: async values => {
      validate(values);
      const ensuredValues = { ...values, group };
      const stageRes = await objectApi.update(ensuredValues);
      if (stageRes.errors) {
        throw new ContextApiError({
          ...stageRes,
          title: `Save broker ${values.name} failed.`,
        });
      }
      const data = generateClusterResponse({ stageValues: stageRes.data });
      const key = getKey(stageRes.data);
      return { ...data, ...key };
    },
    delete: async name => {
      const params = { name, group };
      const res = await brokerApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.remove(params);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      return params;
    },
    start: async name => {
      const params = { name, group };
      await brokerApi.start(params);
      const res = await wait({
        api: new API(RESOURCE.BROKER),
        objectKey: params,
        checkFn: waitForRunning,
        // we don't need to retry too frequently
        sleep: 5000,
      });
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    stop: async name => {
      const params = { name, group };
      const res = await brokerApi.stop(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
  };
};
