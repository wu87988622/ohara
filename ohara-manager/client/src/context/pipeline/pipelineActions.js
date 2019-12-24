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

import * as pipelineApi from 'api/pipelineApi';
import {
  fetchPipelinesRoutine,
  addPipelineRoutine,
  setCurrentPipelineRoutine,
  deletePipelineRoutine,
  updatePipeineRoutine,
} from './pipelineRoutines';

import { hashKey } from 'utils/object';

const createFetchPipelines = (
  state,
  dispatch,
  showMessage,
) => async workspace => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(fetchPipelinesRoutine.request());
  const pipelines = await pipelineApi.getAll({ group: hashKey(workspace) });

  if (pipelines.errors) {
    dispatch(fetchPipelinesRoutine.failure(pipelines.title));
    showMessage(pipelines.title);
    return;
  }

  dispatch(fetchPipelinesRoutine.success(pipelines.data));
};

const createAddPipeline = (state, dispatch, showMessage) => async values => {
  if (state.isFetching) return;

  dispatch(addPipelineRoutine.request());
  const pipeline = await pipelineApi.create(values);

  if (pipeline.errors) {
    dispatch(addPipelineRoutine.failure(pipeline.title));
    showMessage(pipeline.title);
    return;
  }

  dispatch(addPipelineRoutine.success(pipeline.data));
  showMessage(pipeline.title);
};

const createDeletePipeline = (
  state,
  dispatch,
  showMessage,
) => async pipeline => {
  if (state.isFetching) return;

  dispatch(deletePipelineRoutine.request());

  // TODO: stop all connectors before processing to delete pipeline. Tracked in
  // https://github.com/oharastream/ohara/issues/3331

  const deletePipelineResponse = await pipelineApi.remove({
    name: pipeline.name,
    group: pipeline.group,
  });

  if (deletePipelineResponse.errors) {
    dispatch(deletePipelineRoutine.failure(deletePipelineResponse.title));
    showMessage(deletePipelineResponse.title);
    return;
  }

  dispatch(
    deletePipelineRoutine.success({
      name: pipeline.name,
      group: pipeline.group,
    }),
  );
  showMessage(deletePipelineResponse.title);
};

const createUpdatePipeline = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(updatePipeineRoutine.request());

  const updatePipelineResponse = await pipelineApi.update(value);

  if (updatePipelineResponse.errors) {
    dispatch(updatePipeineRoutine.failure(updatePipelineResponse.title));
    showMessage(updatePipelineResponse.title);
    return;
  }

  dispatch(updatePipeineRoutine.success(updatePipelineResponse.data));
  showMessage(updatePipelineResponse.title);
};

const createSetCurrentPipeline = dispatch => pipelineName => {
  dispatch(setCurrentPipelineRoutine.trigger(pipelineName));
};

export {
  createFetchPipelines,
  createDeletePipeline,
  createAddPipeline,
  createSetCurrentPipeline,
  createUpdatePipeline,
};
