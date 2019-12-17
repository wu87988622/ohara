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

import { has, omit, map, get } from 'lodash';

import * as objectApi from 'api/objectApi';
import {
  fetchWorkspacesRoutine,
  addWorkspaceRoutine,
  updateWorkspaceRoutine,
  stageWorkspaceRoutine,
  deleteWorkspaceRoutine,
} from './workspaceRoutines';

const WORKSPACE = 'workspace';

const checkRequired = values => {
  if (!has(values, 'name')) {
    throw new Error("Values is missing required member 'name'");
  }
};

/**
 * Transform an object to a workspace
 *
 * The structure of object in API:
 * {
 *   "name": "abc",
 *   "group": "workspace",
 *   "nodeNames": ["dev1"],
 *   "tags": {
 *     "name": "abc",
 *     "group": "workspace",
 *     "nodeNames": ["dev1", "dev2"],
 *   }
 * }
 *
 * The structure of workspace in UI:
 * {
 *   "settings": {
 *     "name": "abc",
 *     "group": "workspace",
 *     "nodeNames": ["dev1"],
 *   }
 *   "stagingSettings": {
 *     "name": "abc",
 *     "group": "workspace",
 *     "nodeNames": ["dev1", "dev2"],
 *   }
 * }
 */
const transformToWorkspace = object => ({
  settings: omit(object, 'tags'),
  stagingSettings: get(object, 'tags'),
});

const fetchWorkspacesCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchWorkspacesRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;
  try {
    dispatch(routine.request());
    const result = await objectApi.getAll({ group: WORKSPACE });
    if (result.errors) throw new Error(result.title);
    const workspaces = map(result.data, transformToWorkspace);
    dispatch(routine.success(workspaces));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const addWorkspaceCreator = (
  state,
  dispatch,
  showMessage,
  routine = addWorkspaceRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const ensuredValues = { ...values, group: WORKSPACE };
    const params = {
      ...ensuredValues,
      tags: ensuredValues, // Copy from values, used to store the staging data.
    };
    dispatch(routine.request());
    const result = await objectApi.create(params);
    if (result.errors) throw new Error(result.title);
    dispatch(routine.success(transformToWorkspace(result.data)));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const updateWorkspaceCreator = (
  state,
  dispatch,
  showMessage,
  routine = updateWorkspaceRoutine,
) => async values => {
  if (state.isFetching) return;
  try {
    checkRequired(values);
    const ensuredValues = { ...values, group: WORKSPACE };
    const params = {
      ...ensuredValues,
      tags: ensuredValues, // Sync from values
    };
    dispatch(routine.request());
    const result = await objectApi.update(params);
    if (result.errors) throw new Error(result.title);
    dispatch(routine.success(transformToWorkspace(result.data)));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const stageWorkspaceCreator = (
  state,
  dispatch,
  showMessage,
  routine = stageWorkspaceRoutine,
) => async values => {
  if (state.isFetching) return;
  try {
    checkRequired(values);
    const ensuredValues = omit(values, ['name', 'group', 'tags']);
    const params = {
      name: values.name,
      group: WORKSPACE,
      tags: ensuredValues,
    };
    dispatch(routine.request());
    const result = await objectApi.update(params);
    if (result.errors) throw new Error(result.title);
    dispatch(routine.success(transformToWorkspace(result.data)));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const deleteWorkspaceCreator = (
  state,
  dispatch,
  showMessage,
  routine = deleteWorkspaceRoutine,
) => async name => {
  if (state.isFetching) return;
  try {
    const params = {
      name,
      group: WORKSPACE,
    };
    dispatch(routine.request());
    const result = await objectApi.remove(params);
    if (result.errors) throw new Error(result.title);
    dispatch(routine.success(transformToWorkspace(result.data)));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

export {
  fetchWorkspacesCreator,
  addWorkspaceCreator,
  updateWorkspaceCreator,
  stageWorkspaceCreator,
  deleteWorkspaceCreator,
};
