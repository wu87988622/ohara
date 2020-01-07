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

import { map, omit, values } from 'lodash';

import * as objectApi from 'api/objectApi';
import { getKey } from 'utils/object';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { workspaceGroup, showMessage } = context;
  if (!workspaceGroup) return;

  const group = workspaceGroup;
  return {
    fetchAll: async () => {
      const res = await objectApi.getAll({ group });
      if (res.errors) throw new Error(`Fetch workspace list failed.`);

      return map(res.data, object => {
        return generateClusterResponse({
          values: object.tags,
          stageValues: omit(object, 'tags'),
        });
      });
    },
    fetch: async name => {
      const res = await objectApi.get({ name, group });
      if (res.errors) throw new Error(`Fetch workspace ${name} failed.`);

      return generateClusterResponse({
        values: res.data.tags,
        stageValues: omit(res.data, 'tags'),
      });
    },
    create: async values => {
      try {
        validate(values);
        const ensuredValues = { ...values, group };
        // keep a reference in tags
        const res = await objectApi.create({
          ...ensuredValues,
          tags: ensuredValues,
        });
        if (res.errors)
          throw new Error(`Create workspace ${values.name} failed.`);

        const data = generateClusterResponse({
          values: res.data.tags,
          stageValues: omit(res.data, 'tags'),
        });
        showMessage(`Create workspace ${values.name} successful.`);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    update: async values => {
      try {
        validate(values);
        const ensuredValues = { ...values, group, tags: values };
        const res = await objectApi.update(ensuredValues);
        if (res.errors)
          throw new Error(`Save workspace ${values.name} failed.`);

        const data = generateClusterResponse({ values: res.data.tags });
        showMessage(`Save workspace ${values.name} successful.`);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    stage: async values => {
      try {
        validate(values);
        const ensuredValues = { ...values, group };
        const res = await objectApi.update(ensuredValues);
        if (res.errors)
          throw new Error(`Save workspace ${values.name} failed.`);

        const data = generateClusterResponse({ stageValues: res.data });
        const key = getKey(res.data);
        showMessage(`Save workspace ${values.name} successful.`);
        return { ...data, ...key };
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const params = { name, group };
        const res = await objectApi.remove(params);
        if (res.errors) throw new Error(`Delete workspace ${name} failed.`);

        showMessage(`Delete workspace ${values.name} successful.`);
        return params;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
  };
};
