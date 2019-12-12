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
import { fetchNodesRoutine } from './nodeRoutines';

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

const addNodeCreator = () => async () => {
  // TODO: implement the logic for add node
};
const updateNodeCreator = () => async () => {
  // TODO: implement the logic for update node
};
const deleteNodeCreator = () => async () => {
  // TODO: implement the logic for delete node
};

export {
  fetchNodesCreator,
  addNodeCreator,
  updateNodeCreator,
  deleteNodeCreator,
};
