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

import * as inspectApi from 'api/inspectApi';
import ContextApiError from 'context/ContextApiError';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { topicGroup, brokerKey, workspaceKey } = context;
  if (!topicGroup || !brokerKey || !workspaceKey) return;

  const group = topicGroup;
  return {
    fetchData: async values => {
      validate(values);
      const params = {
        key: { name: values.name, group },
        ...values,
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
