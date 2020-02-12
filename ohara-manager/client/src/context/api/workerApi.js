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

import { KIND } from 'const';
import wait from 'api/waitApi';
import * as waitUtil from 'api/utils/waitUtils';
import * as URL from 'api/utils/url';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import * as workerApi from 'api/workerApi';
import ContextApiError from 'context/ContextApiError';
import { getKey } from 'utils/object';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { workerGroup, brokerGroup } = context;
  if (!workerGroup || !brokerGroup) return;

  const group = workerGroup;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await workerApi.getAll(params);
      if (res.errors) throw new ContextApiError(res);
      return await Promise.all(
        map(res.data, async worker => {
          const params = { name: worker.name, group };
          const stageRes = await objectApi.get(params);
          if (stageRes.errors) throw new ContextApiError(stageRes);
          const infoRes = await inspectApi.getWorkerInfo(params);
          if (infoRes.errors) throw new ContextApiError(infoRes);
          return generateClusterResponse({
            values: worker,
            stageValues: stageRes.data,
            inspectInfo: infoRes.data,
          });
        }),
      );
    },
    fetch: async name => {
      const params = { name, group };
      const res = await workerApi.get(params);
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.get(params);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      const infoRes = await inspectApi.getWorkerInfo(params);
      if (infoRes.errors) throw new ContextApiError(infoRes);
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: infoRes.data,
      });
    },
    create: async values => {
      validate(values);
      const brokerClusterKey = { group: brokerGroup, name: values.name };
      const ensuredValues = { ...values, group, brokerClusterKey };
      const res = await workerApi.create(ensuredValues);
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.create(res.data);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      const infoRes = await inspectApi.getWorkerInfo(ensuredValues);
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
      const res = await workerApi.update(ensuredValues);
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
          title: `Save worker ${values.name} failed.`,
        });
      }
      const data = generateClusterResponse({ stageValues: stageRes.data });
      const key = getKey(stageRes.data);
      return { ...data, ...key };
    },
    delete: async name => {
      const params = { name, group };
      const res = await workerApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      const stageRes = await objectApi.remove(params);
      if (stageRes.errors) throw new ContextApiError(stageRes);
      return params;
    },
    start: async name => {
      const params = { name, group };
      const res = await workerApi.start(params);
      if (res.errors) throw new ContextApiError(res);
      // We need to wait the connectors are ready
      // before we assert this worker started actually
      const waitRes = await wait({
        url: `${URL.INSPECT_URL}/${KIND.worker}/${name}?group=${group}`,
        checkFn: waitUtil.waitForConnectReady,
        // we don't need to inspect too frequently
        sleep: 5000,
      });
      if (waitRes.errors) {
        throw new ContextApiError({
          ...waitRes,
          title: `Get connector list of worker ${name} failed.`,
        });
      }
      const infoRes = await inspectApi.getWorkerInfo(params);
      return generateClusterResponse({
        values: res.data,
        inspectInfo: infoRes.data,
      });
    },
    stop: async name => {
      const params = { name, group };
      const res = await workerApi.stop(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
  };
};
