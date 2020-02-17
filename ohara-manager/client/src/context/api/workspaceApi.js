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

import { map, omit } from 'lodash';

import * as objectApi from 'api/objectApi';
import ContextApiError from 'context/ContextApiError';
import { getKey } from 'utils/object';
import { generateClusterResponse, validate } from './utils';

export const createApi = context => {
  const { workspaceGroup } = context;
  if (!workspaceGroup) return;

  const group = workspaceGroup;
  return {
    fetchAll: async () => {
      const res = await objectApi.getAll({ group });
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Fetch workspace list failed.`,
        });
      }
      return map(res.data, object => {
        return generateClusterResponse({
          values: { ...object.tags, ...getKey(object) },
          stageValues: omit(object, 'tags'),
        });
      });
    },
    fetch: async name => {
      const res = await objectApi.get({ name, group });
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Fetch workspace ${name} failed.`,
        });
      }
      return generateClusterResponse({
        values: { ...res.data.tags, ...getKey(res.data) },
        stageValues: omit(res.data, 'tags'),
      });
    },
    create: async values => {
      validate(values);
      const ensuredValues = { ...values, group };
      // keep a reference in tags
      const res = await objectApi.create({
        ...ensuredValues,
        tags: ensuredValues,
      });
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Create workspace ${values.name} failed.`,
        });
      }
      return generateClusterResponse({
        values: res.data.tags,
        stageValues: omit(res.data, 'tags'),
      });
    },
    update: async values => {
      validate(values);
      const ensuredValues = { ...values, group, tags: values };
      const res = await objectApi.update(ensuredValues);
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Save workspace ${values.name} failed.`,
        });
      }
      const data = generateClusterResponse({ values: res.data.tags });
      const key = getKey(res.data);
      return { ...data, ...key };
    },
    stage: async values => {
      validate(values);
      const ensuredValues = { ...values, group };
      const res = await objectApi.update(ensuredValues);
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Save workspace ${values.name} failed.`,
        });
      }
      const data = generateClusterResponse({ stageValues: res.data });
      const key = getKey(res.data);
      return { ...data, ...key };
    },
    delete: async name => {
      const params = { name, group };
      const res = await objectApi.remove(params);
      if (res.errors) {
        throw new ContextApiError({
          ...res,
          title: `Delete workspace ${name} failed.`,
        });
      }
      return params;
    },
  };
};
