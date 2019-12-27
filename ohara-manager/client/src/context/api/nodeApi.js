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

import { has, isEmpty } from 'lodash';
import * as nodeApi from 'api/nodeApi';

const validate = values => {
  if (!has(values, 'hostname'))
    throw new Error('The values must contain the hostname property');
};

export const createApi = context => {
  const { showMessage } = context;
  return {
    fetchAll: async () => {
      const res = await nodeApi.getAll();
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetch: async hostname => {
      const res = await nodeApi.get({ hostname });
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    create: async values => {
      try {
        validate(values);
        const res = await nodeApi.create({ ...values });
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        showMessage(res.title);
        return res.data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    update: async values => {
      try {
        validate(values);
        const res = await nodeApi.update({ ...values });
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        showMessage(res.title);
        return res.data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async hostname => {
      try {
        const params = { hostname };
        const res = await nodeApi.remove(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        showMessage(res.title);
        return params;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
  };
};
