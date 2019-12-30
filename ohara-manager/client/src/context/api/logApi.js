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

import { isEmpty } from 'lodash';
import * as logApi from 'api/logApi';
import { validate } from './utils';
import { hashByGroupAndName } from 'utils/sha';
import { WORKER, BROKER, ZOOKEEPER } from './index';

export const createApi = context => {
  const { workspaceName, pipelineName } = context;
  if (!workspaceName || !pipelineName) return;

  return {
    fetchConfigurator: async values => {
      validate(values);
      const res = await logApi.getConfiguratorLog(values);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetchZookeeper: async values => {
      validate(values);
      const params = { ...values, group: ZOOKEEPER };
      const res = await logApi.getZookeeperLog(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetchBroker: async values => {
      validate(values);
      const params = { ...values, group: BROKER };
      const res = await logApi.getBrokerLog(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetchWorker: async values => {
      validate(values);
      const params = { ...values, group: WORKER };
      const res = await logApi.getWorkerLog(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetchStream: async values => {
      const group = hashByGroupAndName(workspaceName, pipelineName);
      validate(values);
      const params = { ...values, group };
      const res = await logApi.getStreamLog(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
  };
};
