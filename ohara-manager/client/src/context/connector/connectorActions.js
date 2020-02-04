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

import * as routines from './connectorRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, connectorApi } = context;

  return {
    fetchConnectors: async () => {
      const routine = routines.fetchConnectorsRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    createConnector: async values => {
      const routine = routines.createConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.create(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    updateConnector: async values => {
      const routine = routines.updateConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    stageConnector: async values => {
      const routine = routines.stageConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.stage(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    deleteConnector: async name => {
      const routine = routines.deleteConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.delete(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    startConnector: async name => {
      const routine = routines.startConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.start(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    stopConnector: async name => {
      const routine = routines.stopConnectorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await connectorApi.stop(name);
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
