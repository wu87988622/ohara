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
import * as topicApi from 'api/topicApi';
import ContextApiError from 'context/ContextApiError';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { topicGroup, brokerKey, workspaceKey } = context;
  if (!topicGroup || !brokerKey || !workspaceKey) return;

  const group = topicGroup;
  const brokerClusterKey = brokerKey;
  const parentKey = workspaceKey;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await topicApi.getAll(params);
      if (res.errors) throw new ContextApiError(res);
      return await Promise.all(
        map(res.data, async topic => {
          const infoRes = await inspectApi.getBrokerInfo(brokerClusterKey);
          if (infoRes.errors) throw new ContextApiError(infoRes);
          return generateClusterResponse({
            values: topic,
            inspectInfo: infoRes.data.classInfos[0],
          });
        }),
      );
    },
    fetch: async name => {
      const params = { name, group };
      const res = await topicApi.get(params);
      if (res.errors) throw new ContextApiError(res);
      const infoRes = await inspectApi.getBrokerInfo(brokerClusterKey);
      if (infoRes.errors) throw new ContextApiError(infoRes);
      return generateClusterResponse({
        values: res.data,
        inspectInfo: infoRes.data.classInfos[0],
      });
    },
    create: async values => {
      validate(values);
      const params = {
        ...values,
        group,
        brokerClusterKey,
        tags: { ...values.tags, parentKey },
      };
      const res = await topicApi.create(params);
      if (res.errors) throw new ContextApiError(res);
      const infoRes = await inspectApi.getBrokerInfo(brokerClusterKey);
      if (infoRes.errors) throw new ContextApiError(infoRes);
      return generateClusterResponse({
        values: res.data,
        inspectInfo: infoRes.data.classInfos[0],
      });
    },
    update: async values => {
      validate(values);
      const params = { ...values, group };
      const res = await topicApi.update(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    delete: async name => {
      const params = { name, group };
      const res = await topicApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      return params;
    },
    start: async name => {
      const params = { name, group };
      const res = await topicApi.start(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    stop: async name => {
      const params = { name, group };
      const res = await topicApi.stop(params);
      if (res.errors) throw new ContextApiError(res);
      return generateClusterResponse({ values: res.data });
    },
    fetchData: async values => {
      validate(values);
      const params = {
        ...values,
        group,
      };
      const res = await inspectApi.getTopicData(params);
      if (res.errors) throw new ContextApiError(res);
      const noTagsData = res.data.messages.map(message => {
        // we don't need the "tags" field in the topic data
        if (message.value) delete message.value.tags;
        return message;
      });
      return generateClusterResponse({
        values: { messages: noTagsData },
      });
    },
  };
};
