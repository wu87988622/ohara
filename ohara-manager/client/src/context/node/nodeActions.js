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

import * as routines from './nodeRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, nodeApi } = context;
  return {
    fetchNodes: async () => {
      const routine = routines.fetchNodesRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await nodeApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    createNode: async values => {
      const routine = routines.createNodeRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await nodeApi.create(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    updateNode: async values => {
      const routine = routines.updateNodeRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await nodeApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    deleteNode: async hostname => {
      const routine = routines.deleteNodeRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await nodeApi.delete(hostname);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    refreshNodes: async () => {
      const routine = routines.fetchNodesRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await nodeApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
  };
};
