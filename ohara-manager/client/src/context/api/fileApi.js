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

import * as fileApi from 'api/fileApi';
import ContextApiError from 'context/ContextApiError';
import { validate } from './utils';

export const createApi = context => {
  const { fileGroup, workspaceKey } = context;
  if (!fileGroup || !workspaceKey) return;

  const group = fileGroup;
  const parentKey = workspaceKey;

  return {
    fetchAll: async () => {
      const params = { group };
      const res = await fileApi.getAll(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    fetch: async name => {
      const params = { name, group };
      const res = await fileApi.get(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    create: async file => {
      const params = { file, group, tags: { parentKey } };
      const res = await fileApi.create(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    update: async values => {
      validate(values);
      const params = { ...values, group };
      const res = await fileApi.update(params);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    delete: async name => {
      const params = { name, group };
      const res = await fileApi.remove(params);
      if (res.errors) throw new ContextApiError(res);
      return params;
    },
  };
};
