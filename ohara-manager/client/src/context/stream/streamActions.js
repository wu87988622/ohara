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

import * as routines from './streamRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, streamApi } = context;
  return {
    fetchStreams: async () => {
      const routine = routines.fetchStreamsRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    createStream: async values => {
      const routine = routines.createStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.create(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    updateStream: async values => {
      const routine = routines.updateStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    deleteStream: async name => {
      const routine = routines.deleteStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.delete(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    startStream: async name => {
      const routine = routines.startStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.start(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    stopStream: async name => {
      const routine = routines.stopStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await streamApi.stop(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
  };
};
