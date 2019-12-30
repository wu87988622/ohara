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

import * as routines from './pipelineRoutines';
import * as action from 'utils/action';

export const createActions = context => {
  const { state, dispatch, pipelineApi } = context;
  return {
    fetchPipelines: async () => {
      const routine = routines.fetchPipelinesRoutine;
      if (state.isFetching || state.lastUpdated || state.error) return;
      try {
        dispatch(routine.request());
        const data = await pipelineApi.fetchAll();
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    createPipeline: async values => {
      const routine = routines.createPipelineRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await pipelineApi.create(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    updatePipeline: async values => {
      const routine = routines.updatePipelineRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await pipelineApi.update(values);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    deletePipeline: async name => {
      const routine = routines.deletePipelineRoutine;
      if (state.isFetching) return;
      try {
        dispatch(routine.request());
        const data = await pipelineApi.delete(name);
        dispatch(routine.success(data));
        return action.success(data);
      } catch (e) {
        dispatch(routine.failure(e.message));
        return action.failure(e.message);
      }
    },
    setCurrentPipeline: pipelineName => {
      dispatch(routines.setCurrentPipelineRoutine.trigger(pipelineName));
    },
    setSelectedCell: cellName => {
      dispatch(routines.setSelectedCellRoutine.trigger(cellName));
    },
  };
};
