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

import * as routines from './topicDataRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, topicApi } = context;
  return {
    refetchTopic: () => {
      if (state.isFetching) return;
      dispatch(routines.refetchTopicRoutine.trigger());
    },
    fetchTopicData: async ({ name, limit, timeout = 3000 }) => {
      const routine = routines.fetchTopicDataRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await topicApi.fetchData({ name, limit, timeout });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e.getPayload());
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    setName: name => {
      const routine = routines.setNameRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ name }));
    },
    setLimit: limit => {
      const routine = routines.setLimitRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ limit }));
    },
  };
};
