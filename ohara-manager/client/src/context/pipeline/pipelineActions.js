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

import { isEmpty } from 'lodash';

import * as pipelineApi from 'api/pipelineApi';
import { fetchPipelinesRoutine, addPipelineRoutine } from './pipelineRoutines';

const createFetchPipelines = (
  state,
  dispatch,
  showMessage,
) => async workspaceName => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(fetchPipelinesRoutine.request());
  const pipelines = await pipelineApi.getAll({ group: workspaceName });

  if (!pipelines) {
    const error = 'failed to fetch pipelines';
    dispatch(fetchPipelinesRoutine.failure(error));
    showMessage(error);
    return;
  }

  dispatch(fetchPipelinesRoutine.success(pipelines));
};

const createAddPipeline = (state, dispatch, showMessage) => async values => {
  if (state.isFetching) return;

  const { name } = values;

  dispatch(addPipelineRoutine.request());
  const pipeline = await pipelineApi.create(values);

  if (isEmpty(pipeline)) {
    const error = `Failed to add pipeline ${name}`;
    dispatch(addPipelineRoutine.failure(error));
    showMessage(error);
    return;
  }

  dispatch(addPipelineRoutine.success(pipeline));
  showMessage(`Successfully added pipeline ${name}`);
};

export { createFetchPipelines, createAddPipeline };
