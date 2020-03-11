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

import * as objectApi from 'api/objectApi';
import * as pipelineApi from 'api/pipelineApi';

const workspaceGroup = 'workspace';

export const wrapFetchWorkspaces = () =>
  new Promise((resolve, reject) =>
    objectApi.getAll({ group: workspaceGroup }).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapCreateWorkspace = values =>
  new Promise((resolve, reject) =>
    objectApi.create(values).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapUpdateWorkspace = values =>
  new Promise((resolve, reject) =>
    objectApi.update(values).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapFetchPipelines = () =>
  new Promise((resolve, reject) =>
    pipelineApi.getAll().then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapFetchPipeline = params =>
  new Promise((resolve, reject) =>
    pipelineApi.get(params).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapCreatePipeline = values =>
  new Promise((resolve, reject) =>
    pipelineApi.create(values).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapUpdatePipeline = values =>
  new Promise((resolve, reject) =>
    pipelineApi.update(values).then(res => {
      if (res.errors) reject(res);
      else resolve(res);
    }),
  );

export const wrapDeletePipeline = params =>
  new Promise((resolve, reject) =>
    pipelineApi.remove(params).then(res => {
      if (res.errors) reject(res);
      else resolve(params);
    }),
  );
