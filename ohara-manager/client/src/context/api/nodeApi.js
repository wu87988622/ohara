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

import { has } from 'lodash';
import * as nodeApi from 'api/nodeApi';
import ContextApiError from 'context/ContextApiError';

const validate = values => {
  if (!has(values, 'hostname'))
    throw new ContextApiError({
      title: 'The values must contain the hostname property',
    });
};

export const createApi = () => {
  return {
    fetchAll: async () => {
      const res = await nodeApi.getAll();
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    fetch: async hostname => {
      const res = await nodeApi.get(hostname);
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    create: async values => {
      validate(values);
      const res = await nodeApi.create({ ...values });
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    update: async values => {
      validate(values);
      const res = await nodeApi.update({ ...values });
      if (res.errors) throw new ContextApiError(res);
      return res.data;
    },
    delete: async hostname => {
      const params = { hostname };
      const res = await nodeApi.remove(hostname);
      if (res.errors) throw new ContextApiError(res);
      return params;
    },
  };
};
