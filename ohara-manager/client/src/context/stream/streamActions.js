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

import * as streamApi from 'api/streamApi';
import {
  fetchStreamsRoutine,
  addStreamRoutine,
  deleteStreamRoutine,
  updateStreamRoutine,
} from './streamRoutines';

const fetchStreamsCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchStreamsRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(routine.request());
  const result = await streamApi.getAll();

  if (result.errors) {
    dispatch(routine.failure(result.title));
    showMessage(result.title);
    return;
  }

  dispatch(routine.success(result.data));
};

const addStreamCreator = (state, dispatch, showMessage) => async (
  value,
  body,
) => {
  if (state.isFetching) return;

  dispatch(addStreamRoutine.request());
  const createStreamResponse = await streamApi.create(value, body);

  // Failed to create, show a custom error message
  if (createStreamResponse.errors) {
    dispatch(addStreamRoutine.failure(createStreamResponse.title));
    showMessage(createStreamResponse.title);
    return;
  }

  // Node successfully created, display success message
  dispatch(addStreamRoutine.success(createStreamResponse.data));
  showMessage(createStreamResponse.title);
};
const updateStreamCreator = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(updateStreamRoutine.request());

  const updateStreamResponse = await streamApi.update(value);

  if (updateStreamResponse.errors) {
    dispatch(updateStreamRoutine.failure(updateStreamResponse.title));
    showMessage(updateStreamResponse.title);
    return;
  }

  dispatch(updateStreamRoutine.success(value));
  showMessage(updateStreamResponse.title);
};
const deleteStreamCreator = (state, dispatch, showMessage) => async value => {
  if (state.isFetching) return;

  dispatch(deleteStreamRoutine.request());

  const deleteStreamResponse = await streamApi.remove(value);

  if (deleteStreamResponse.errors) {
    dispatch(deleteStreamRoutine.failure(deleteStreamResponse.title));
    showMessage(deleteStreamResponse.title);
    return;
  }

  dispatch(deleteStreamRoutine.success(value));
  showMessage(deleteStreamResponse.title);
};

export {
  fetchStreamsCreator,
  addStreamCreator,
  updateStreamCreator,
  deleteStreamCreator,
};
