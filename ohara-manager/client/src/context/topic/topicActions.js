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

import { get, has, isEmpty } from 'lodash';

import * as topicApi from 'api/topicApi';
import {
  fetchTopicsRoutine,
  addTopicRoutine,
  deleteTopicRoutine,
} from './topicRoutines';

const createFetchTopics = (state, dispatch) => async workspaceName => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(fetchTopicsRoutine.request());
  const topics = await topicApi.getAll({ group: workspaceName });

  if (isEmpty(topics)) {
    dispatch(fetchTopicsRoutine.failure('failed to fetch topics'));
    return;
  }

  dispatch(fetchTopicsRoutine.success(topics));
};

const createAddTopic = (state, dispatch, showMessage) => async values => {
  if (state.isFetching) return;

  const { name: topicName, group: topicGroup } = values;

  dispatch(addTopicRoutine.request());
  const createTopicResponse = await topicApi.create(values);

  const isCreated = !isEmpty(createTopicResponse);

  // Failed to create, show a custom error message
  if (!isCreated) {
    const error = `Failed to add topic ${topicName}`;
    dispatch(addTopicRoutine.failure(error));
    // After the error handling logic is done in https://github.com/oharastream/ohara/issues/3124
    // we can remove this custom message since it's handled higher up in the API layer
    showMessage(error);
    return;
  }

  const startTopicResponse = await topicApi.start({
    name: topicName,
    group: topicGroup,
  });

  const isStarted = get(startTopicResponse, 'state', undefined);

  // Failed to start, show a custom error message here
  if (!isStarted) {
    const error = `Failed to start topic ${topicName}`;
    dispatch(addTopicRoutine.failure(error));
    // After the error handling logic is done in https://github.com/oharastream/ohara/issues/3124
    // we can remove this custom message since it's handled higher up in the API layer
    showMessage(error);
    return;
  }

  // Topic successfully created, display success message
  dispatch(addTopicRoutine.success(createTopicResponse));
  showMessage(`Successfully added topic ${topicName}`);
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

  const isStopped = !has(stopTopicResponse, 'state');

  // Failed to start, show a custom error message here
  if (!isStopped) {
    const error = `Failed to stop topic ${topicName}`;
    dispatch(deleteTopicRoutine.failure(error));
    showMessage(error);
    return;
  }

  const deleteTopicResponse = await topicApi.remove({
    name: topicName,
    group: topicGroup,
  });
  const isDeleted = !isEmpty(deleteTopicResponse);

  if (!isDeleted) {
    const error = `Failed to delete topic ${topicName}`;
    dispatch(deleteTopicRoutine.failure(error));
    showMessage(error);
    return;
  }

  dispatch(
    deleteTopicRoutine.success({
      name: topicName,
      group: topicGroup,
    }),
  );
  showMessage(`Successfully deleted topic ${topicName}`);
};

export { createFetchTopics, createAddTopic, createDeleteTopic };
