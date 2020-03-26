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

import * as routines from './logRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, eventLog, logApi } = context;
  return {
    refetchLog: () => {
      if (state.isFetching) return;
      dispatch(routines.refetchLogRoutine.trigger());
    },
    fetchConfiguratorLog: async sinceSeconds => {
      const routine = routines.fetchConfiguratorRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await logApi.fetchConfigurator({ sinceSeconds });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    fetchZookeeperLog: async sinceSeconds => {
      const routine = routines.fetchZookeeperRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await logApi.fetchZookeeper({ sinceSeconds });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    fetchBrokerLog: async sinceSeconds => {
      const routine = routines.fetchBrokerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await logApi.fetchBroker({ sinceSeconds });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    fetchWorkerLog: async sinceSeconds => {
      const routine = routines.fetchWorkerRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await logApi.fetchWorker({ sinceSeconds });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    fetchStreamLog: async ({ name, sinceSeconds }) => {
      const routine = routines.fetchStreamRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await logApi.fetchStream({ name, sinceSeconds });
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        eventLog.error(e);
        dispatch(routine.failure(e.data.error));
        return action.failure(e.data.error);
      }
    },
    setLogType: logType => {
      const routine = routines.setLogTypeRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ logType }));
    },
    setHostName: hostName => {
      const routine = routines.setHostNameRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ hostName }));
    },
    setStreamName: streamName => {
      const routine = routines.setStreamNameRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ streamName }));
    },
    setTimeGroup: timeGroup => {
      const routine = routines.setTimeGroupRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ timeGroup }));
    },
    setTimeRange: timeRange => {
      const routine = routines.setTimeRangeRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ timeRange }));
    },
    setStartTime: startTime => {
      const routine = routines.setStartTimeRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ startTime }));
    },
    setEndTime: endTime => {
      const routine = routines.setEndTimeRoutine;
      if (state.isFetching) return;
      dispatch(routine.request({ endTime }));
    },
  };
};
