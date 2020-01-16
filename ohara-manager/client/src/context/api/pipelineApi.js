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
import * as pipelineApi from 'api/pipelineApi';
import { validate } from './utils';

export const createApi = context => {
  const { pipelineGroup, showMessage, createEventLog } = context;
  if (!pipelineGroup) return;

  const group = pipelineGroup;
  return {
    fetchAll: async () => {
      const params = { group };
      const res = await pipelineApi.getAll(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    fetch: async name => {
      const params = { name, group };
      const res = await pipelineApi.get(params);
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return res.data;
    },
    create: async values => {
      try {
        validate(values);
        const params = { ...values, group };
        const res = await pipelineApi.create(params);
        if (!isEmpty(res.errors)) {
          createEventLog(res, 'error');
          throw new Error(res.title);
        }
        createEventLog(res);
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
        const params = { ...values, group };
        const res = await pipelineApi.update(params);
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
    delete: async name => {
      try {
        const params = { name, group };
        const res = await pipelineApi.remove(params);
        if (!isEmpty(res.errors)) {
          createEventLog(res, 'error');
          throw new Error(res.title);
        }
        createEventLog(res);
        showMessage(res.title);
        return params;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    refresh: async name => {
      try {
        const params = { name, group };
        const res = await pipelineApi.refresh(params);
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        showMessage(res.title);
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
  };
};
