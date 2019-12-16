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

import * as topicApi from 'api/topicApi';
import { hashKey } from 'utils/object';
import {
  fetchTopicsRoutine,
  addTopicRoutine,
  deleteTopicRoutine,
} from './topicRoutines';

const createFetchTopics = (state, dispatch, showMessage) => async workspace => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(fetchTopicsRoutine.request());
  const result = await topicApi.getAll({ group: hashKey(workspace) });

  if (result.errors) {
    dispatch(fetchTopicsRoutine.failure(result.title));
    showMessage(result.title);
    return;
  }

  dispatch(fetchTopicsRoutine.success(result.data));
};

const createAddTopic = (state, dispatch, showMessage) => async values => {
  if (state.isFetching) return;

  const { name: topicName, group: topicGroup } = values;

  dispatch(addTopicRoutine.request());
  const createTopicResponse = await topicApi.create(values);

  // Failed to create, show a custom error message
  if (createTopicResponse.errors) {
    dispatch(addTopicRoutine.failure(createTopicResponse.title));
    showMessage(createTopicResponse.title);
    return;
  }

  const startTopicResponse = await topicApi.start({
    name: topicName,
    group: topicGroup,
  });

  // Failed to start, show a custom error message here
  if (startTopicResponse.errors) {
    dispatch(addTopicRoutine.failure(startTopicResponse.title));
    showMessage(startTopicResponse.title);
    return;
  }

  // Topic successfully created, display success message
  dispatch(addTopicRoutine.success(createTopicResponse.data));
  showMessage(createTopicResponse.title);
};

const createDeleteTopic = (state, dispatch, showMessage) => async (
  topicName,
  topicGroup,
) => {
  if (state.isFetching) return;

  dispatch(deleteTopicRoutine.request());
  const stopTopicResponse = await topicApi.stop({
    name: topicName,
    group: topicGroup,
  });

  // Failed to start, show a custom error message here
  if (stopTopicResponse.errors) {
    dispatch(deleteTopicRoutine.failure(stopTopicResponse.title));
    showMessage(stopTopicResponse.title);
    return;
  }

  const deleteTopicResponse = await topicApi.remove({
    name: topicName,
    group: topicGroup,
  });

  if (deleteTopicResponse.errors) {
    dispatch(deleteTopicRoutine.failure(deleteTopicResponse.title));
    showMessage(deleteTopicResponse.title);
    return;
  }

  dispatch(
    deleteTopicRoutine.success({
      name: topicName,
      group: topicGroup,
    }),
  );
  showMessage(deleteTopicResponse.title);
};

export { createFetchTopics, createAddTopic, createDeleteTopic };
