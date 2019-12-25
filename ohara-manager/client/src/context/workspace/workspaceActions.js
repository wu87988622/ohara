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

import {
  fetchWorkspacesRoutine,
  createWorkspaceRoutine,
  updateWorkspaceRoutine,
  stageWorkspaceRoutine,
  deleteWorkspaceRoutine,
} from './workspaceRoutines';

export const createActions = context => {
  const { state, dispatch, workspaceApi } = context;
  return {
    fetchWorkspaces: async () => {
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(fetchWorkspacesRoutine.request());
        const data = await workspaceApi.fetchAll();
        dispatch(fetchWorkspacesRoutine.success(data));
      } catch (e) {
        dispatch(fetchWorkspacesRoutine.failure(e.message));
      }
    },
    createWorkspace: async values => {
      if (state.isFetching) return;
      try {
        dispatch(createWorkspaceRoutine.request());
        const data = await workspaceApi.create(values);
        dispatch(createWorkspaceRoutine.success(data));
      } catch (e) {
        dispatch(createWorkspaceRoutine.failure(e.message));
      }
    },
    updateWorkspace: async values => {
      if (state.isFetching) return;
      try {
        dispatch(updateWorkspaceRoutine.request());
        const data = await workspaceApi.update(values);
        dispatch(updateWorkspaceRoutine.success(data));
      } catch (e) {
        dispatch(updateWorkspaceRoutine.failure(e.message));
      }
    },
    stageWorkspace: async values => {
      if (state.isFetching) return;
      try {
        dispatch(stageWorkspaceRoutine.request());
        const data = await workspaceApi.stage(values);
        dispatch(stageWorkspaceRoutine.success(data));
      } catch (e) {
        dispatch(stageWorkspaceRoutine.failure(e.message));
      }
    },
    deleteWorkspace: async name => {
      if (state.isFetching) return;
      try {
        dispatch(deleteWorkspaceRoutine.request());
        const data = await workspaceApi.remove(name);
        dispatch(deleteWorkspaceRoutine.success(data));
      } catch (e) {
        dispatch(deleteWorkspaceRoutine.failure(e.message));
      }
    },
  };
};
