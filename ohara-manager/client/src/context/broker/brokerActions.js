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

import { map } from 'lodash';
import * as routines from './brokerRoutines';

export const createActions = context => {
  const { state, dispatch, brokerApi } = context;
  return {
    fetchBrokers: async () => {
      const routine = routines.fetchBrokersRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const brokers = await brokerApi.fetchAll();
        const data = map(brokers, broker => ({
          ...broker,
          serviceType: 'broker',
        }));
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    createBroker: async values => {
      const routine = routines.createBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const createRes = await brokerApi.create(values);
        const startRes = await brokerApi.start(values.name);
        const data = { ...createRes, ...startRes };
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    updateBroker: async values => {
      const routine = routines.updateBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await brokerApi.update(values);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    stageBroker: async values => {
      const routine = routines.stageBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await brokerApi.stage(values);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    deleteBroker: async name => {
      const routine = routines.deleteBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await brokerApi.delete(name);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    startBroker: async name => {
      const routine = routines.startBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await brokerApi.start(name);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
    stopBroker: async name => {
      const routine = routines.stopBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await brokerApi.stop(name);
        dispatch(routine.success(data));
      } catch (e) {
        dispatch(routine.failure(e.message));
      }
    },
  };
};
