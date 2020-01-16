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

import { get, has } from 'lodash';
import localForage from 'localforage';
import * as routines from './eventLogRoutines';

export const createActions = context => {
  const { state, dispatch } = context;
  return {
    fetchEventLogs: async () => {
      const routine = routines.fetchEventLogsRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      dispatch(routine.request());
      const logs = [];
      localForage
        .iterate((value, key) => {
          logs.push({
            key,
            type: get(value, 'type'),
            title: get(value, 'payload.title'),
            createAt: get(value, 'createAt'),
            payload: get(value, 'payload'),
          });
        })
        .then(() => dispatch(routine.success(logs)))
        .catch(err => dispatch(routine.failure(err)));
    },
    createEventLog: async (log, type = 'info') => {
      const routine = routines.createEventLogRoutine;
      if (state.isFetching) return;

      if (!has(log, 'title')) {
        // eslint-disable-next-line no-console
        console.error('createEventLog error: must contain the title property');
        return;
      }

      const now = new Date();
      const key = now.getTime().toString();
      const value = {
        key,
        type,
        title: log.title,
        createAt: now,
        payload: log,
      };
      localForage
        .setItem(key, value)
        .then(data => dispatch(routine.success(data)))
        .catch(err => dispatch(routine.failure(err)));
    },
    clearEventLogs: async () => {
      const routine = routines.clearEventLogsRoutine;
      if (state.isFetching) return;
      localForage
        .clear()
        .then(() => dispatch(routine.success()))
        .catch(err => dispatch(routine.failure(err)));
    },
  };
};
