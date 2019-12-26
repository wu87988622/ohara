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

import { isEmpty, values, map } from 'lodash';
import * as objectApi from 'api/objectApi';
import { generateClusterResponse, stageGroup, validate } from './utils';
import { WORKSPACE } from './index';

const STAGE_GROUP = stageGroup(WORKSPACE);

export const createApi = context => {
  const { showMessage } = context;
  return {
    fetchAll: async () => {
      const res = await objectApi.getAll({ group: WORKSPACE });
      if (!isEmpty(res.errors)) {
        throw new Error(`Fetch workspace list failed.`);
      }
      return await Promise.all(
        map(res.data, async workspace => {
          const stageRes = await objectApi.get({
            name: workspace.name,
            group: STAGE_GROUP,
          });
          if (!isEmpty(stageRes.errors)) {
            throw new Error(`Fetch workspace list failed.`);
          }
          return generateClusterResponse({
            values: workspace,
            stageValues: stageRes.data,
          });
        }),
      );
    },
    fetch: async name => {
      const res = await objectApi.get({ name, group: WORKSPACE });
      if (!isEmpty(res.errors)) {
        throw new Error(`Fetch workspace ${name} failed.`);
      }
      const stageRes = await objectApi.get({
        name,
        group: STAGE_GROUP,
      });
      if (!isEmpty(stageRes.errors)) {
        throw new Error(`Fetch workspace ${name} failed.`);
      }
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
      });
    },
    create: async values => {
      try {
        validate(values);
        const res = await objectApi.create({ ...values, group: WORKSPACE });
        if (!isEmpty(res.errors)) {
          throw new Error(`Create workspace ${values.name} failed.`);
        }
        const stageRes = await objectApi.create({
          ...values,
          group: STAGE_GROUP,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Create workspace ${values.name} failed.`);
        }
        const data = generateClusterResponse({
          values: res.data,
          stageValues: stageRes.data,
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
        const res = await objectApi.update({ ...values, group: WORKSPACE });
        if (!isEmpty(res.errors)) {
          throw new Error(`Save workspace ${values.name} failed.`);
        }
        const data = generateClusterResponse({ values: res.data });
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
        const stageRes = await objectApi.update({
          ...values,
          group: STAGE_GROUP,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Save workspace ${values.name} failed.`);
        }
        const data = generateClusterResponse({ stageValues: stageRes.data });
        showMessage(`Save workspace ${values.name} successful.`);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const res = await objectApi.remove({ name, group: WORKSPACE });
        if (!isEmpty(res.errors)) {
          throw new Error(`Delete workspace ${name} failed.`);
        }
        const stageRes = await objectApi.remove({
          name,
          group: STAGE_GROUP,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Delete workspace ${name} failed.`);
        }
        showMessage(`Delete workspace ${values.name} successful.`);
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
  };
};
