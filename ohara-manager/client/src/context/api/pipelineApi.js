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

import * as pipelineApi from 'api/pipelineApi';
import ContextApiError from 'context/ContextApiError';
import { validate } from './utils';

export const createApi = context => {
  const { pipelineGroup } = context;
  if (!pipelineGroup) return;

  const group = pipelineGroup;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await pipelineApi.getAll(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    fetch: async name => {
      const params = { name, group };
      const res = await pipelineApi.get(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    create: async values => {
      validate(values);
      const params = { ...values, group };
      const res = await pipelineApi.create(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    update: async values => {
      validate(values);
      const params = { ...values, group };
      const res = await pipelineApi.update(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    delete: async name => {
      const params = { name, group };
      const res = await pipelineApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      return params;
    },
    refresh: async name => {
      const params = { name, group };
      const res = await pipelineApi.refresh(params);
      if (res.errors) throw new ContextApiError(res);
    },
  };
};
