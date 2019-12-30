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

import * as routines from './workspaceRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, workspaceApi } = context;
  return {
    fetchWorkspaces: async () => {
      const routine = routines.fetchWorkspacesRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await workspaceApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    createWorkspace: async values => {
      const routine = routines.createWorkspaceRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workspaceApi.create(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    updateWorkspace: async values => {
      const routine = routines.updateWorkspaceRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workspaceApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    stageWorkspace: async values => {
      const routine = routines.stageWorkspaceRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workspaceApi.stage(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    deleteWorkspace: async name => {
      const routine = routines.deleteWorkspaceRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workspaceApi.delete(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
  };
};
