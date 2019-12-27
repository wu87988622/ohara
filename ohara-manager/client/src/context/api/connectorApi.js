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

import { isEmpty, has, map } from 'lodash';
import * as connectorApi from 'api/connectorApi';
import * as objectApi from 'api/objectApi';
import * as inspectApi from 'api/inspectApi';
import { generateClusterResponse } from './utils';
import { WORKER } from './index';
import { hashByGroupAndName } from 'utils/sha';

const validateName = values => {
  if (!has(values, 'name'))
    throw new Error('The request should contain name in payload');
};

const validateClassName = values => {
  if (!has(values, 'connector__class'))
    throw new Error('The request should contain connector__class in payload');
};

export const createApi = context => {
  const { workspaceName, pipelineName, showMessage } = context;
  const connectorGroup = hashByGroupAndName(workspaceName, pipelineName);

  const getDefinition = async className => {
    const workerInfo = await inspectApi.getWorkerInfo({
      group: WORKER,
      name: workspaceName,
    });
    if (!isEmpty(workerInfo.errors)) {
      throw new Error(workerInfo.title);
    }
    const connectorDefinition = workerInfo.data.classInfos
      // the "connector__class" field is converted by "connector.class" from request
      // each connector creation must assign connector.class
      .find(param => param.className === className);
    if (!connectorDefinition)
      throw new Error(`Cannot find required definitions of ${className}.`);

    return connectorDefinition;
  };

  return {
    create: async values => {
      try {
        validateName(values);
        validateClassName(values);
        const res = await connectorApi.create({
          ...values,
          group: connectorGroup,
          workerClusterKey: { group: WORKER, name: workspaceName },
        });
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const stageRes = await objectApi.create({
          ...values,
          group: connectorGroup,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Create connector stage ${values.name} failed.`);
        }
        const info = await getDefinition(values.connector__class);
        const data = generateClusterResponse({
          values: res.data,
          stageValues: stageRes.data,
          inspectInfo: info,
        });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    update: async values => {
      try {
        validateName(values);
        const res = await connectorApi.update({
          ...values,
          group: connectorGroup,
        });
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const data = generateClusterResponse({ values: res.data });
        showMessage(res.title);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    stage: async values => {
      try {
        validateName(values);
        const stageRes = await objectApi.update({
          ...values,
          group: connectorGroup,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Save connector stage ${values.name} failed.`);
        }
        const data = generateClusterResponse({ stageValues: stageRes.data });
        showMessage(`Save connector stage ${values.name} successful.`);
        return data;
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    delete: async name => {
      try {
        const res = await connectorApi.remove({ name, group: connectorGroup });
        if (!isEmpty(res.errors)) {
          throw new Error(res.title);
        }
        const stageRes = await objectApi.remove({
          name,
          group: connectorGroup,
        });
        if (!isEmpty(stageRes.errors)) {
          throw new Error(`Remove connector stage ${name} failed.`);
        }
        showMessage(res.title);
      } catch (e) {
        showMessage(e.message);
        throw e;
      }
    },
    fetch: async name => {
      const res = await connectorApi.get({ name, group: connectorGroup });
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      const stageRes = await objectApi.get({
        name,
        group: connectorGroup,
      });
      if (!isEmpty(stageRes.errors)) {
        throw new Error(res.title);
      }
      const info = await getDefinition(res.data.connector__class);
      return generateClusterResponse({
        values: res.data,
        stageValues: stageRes.data,
        inspectInfo: info,
      });
    },
    fetchAll: async () => {
      const res = await connectorApi.getAll({ group: connectorGroup });
      if (!isEmpty(res.errors)) {
        throw new Error(res.title);
      }
      return await Promise.all(
        map(res.data, async workspace => {
          const stageRes = await objectApi.get({
            name: workspace.name,
            group: connectorGroup,
          });
          if (!isEmpty(stageRes.errors)) {
            throw new Error(`Fetch connector stage list failed.`);
          }
          const info = await getDefinition(workspace.connector__class);
          return generateClusterResponse({
            values: workspace,
            stageValues: stageRes.data,
            inspectInfo: info,
          });
        }),
      );
    },
  };
};
