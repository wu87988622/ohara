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

import { KIND } from 'const';
import * as routines from './workerRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, workerApi } = context;
  return {
    fetchWorkers: async () => {
      const routine = routines.fetchWorkersRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const workers = await workerApi.fetchAll();
        const data = map(workers, worker => ({
          ...worker,
          serviceType: KIND.worker,
        }));
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    createWorker: async values => {
      const routine = routines.createWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const createRes = await workerApi.create(values);
        const startRes = await workerApi.start(values.name);
        // omit startRes "stageSettings", we need the createRes "stageSettings" creation payload
        // omit createRes "classInfos", we need the startRes "classInfos" creation payload
        // to decide workspace is dirties or not
        const data = {
          ...omit(createRes, ['classInfos']),
          ...omit(startRes, ['stagingSettings']),
        };

        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    updateWorker: async values => {
      const routine = routines.updateWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workerApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    stageWorker: async values => {
      const routine = routines.stageWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workerApi.stage(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    deleteWorker: async name => {
      const routine = routines.deleteWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workerApi.delete(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    startWorker: async name => {
      const routine = routines.startWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workerApi.start(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    stopWorker: async name => {
      const routine = routines.stopWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await workerApi.stop(name);
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
