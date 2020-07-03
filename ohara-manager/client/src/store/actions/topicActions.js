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

import { createRoutine } from 'redux-routines';

export const fetchTopics = createRoutine('FETCH_TOPICS');
export const createTopic = createRoutine('CREATE_TOPIC');
export const updateTopic = createRoutine('UPDATE_TOPIC');
export const deleteTopic = createRoutine('DELETE_TOPIC');
export const deleteTopics = createRoutine('DELETE_TOPICS');
export const startTopic = createRoutine('START_TOPIC');
export const startTopics = createRoutine('START_TOPICS');
export const stopTopic = createRoutine('STOP_TOPIC');
export const stopTopics = createRoutine('STOP_TOPICS');

export const createAndStartTopic = createRoutine('CREATE_AND_START_TOPIC');
export const stopAndDeleteTopic = createRoutine('STOP_AND_DELETE_TOPIC');
