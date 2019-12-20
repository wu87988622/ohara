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

import * as nodeApi from 'api/nodeApi';
import {
  fetchNodesRoutine,
  addNodeRoutine,
  deleteNodeRoutine,
  updateNodeRoutine,
} from './nodeRoutines';

const fetchNodesCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchNodesRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(routine.request());
  const result = await nodeApi.getAll();

  if (result.errors) {
    dispatch(routine.failure(result.title));
    showMessage(result.title);
    return;
  }

  dispatch(routine.success(result.data));
};

const addNodeCreator = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(addNodeRoutine.request());
  const createNodeResponse = await nodeApi.create(value);

  // Failed to create, show a custom error message
  if (createNodeResponse.errors) {
    dispatch(addNodeRoutine.failure(createNodeResponse.title));
    showMessage(createNodeResponse.title);
    return;
  }

  // Node successfully created, display success message
  dispatch(addNodeRoutine.success(createNodeResponse.data));
  showMessage(createNodeResponse.title);
};
const updateNodeCreator = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(updateNodeRoutine.request());

  const updateNodeResponse = await nodeApi.update(value);

  if (updateNodeResponse.errors) {
    dispatch(updateNodeRoutine.failure(updateNodeResponse.title));
    showMessage(updateNodeResponse.title);
    return;
  }

  dispatch(updateNodeRoutine.success(value));
  showMessage(updateNodeResponse.title);
};
const deleteNodeCreator = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(deleteNodeRoutine.request());

  const deleteNodeResponse = await nodeApi.remove(value);

  if (deleteNodeResponse.errors) {
    dispatch(deleteNodeRoutine.failure(deleteNodeResponse.title));
    showMessage(deleteNodeResponse.title);
    return;
  }

  dispatch(deleteNodeRoutine.success(value));
  showMessage(deleteNodeResponse.title);
};

export {
  fetchNodesCreator,
  addNodeCreator,
  updateNodeCreator,
  deleteNodeCreator,
};
