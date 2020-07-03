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

import { combineEpics } from 'redux-observable';
import fetchTopicsEpic from './fetchTopicsEpic';
import createTopicEpic from './createTopicEpic';
import deleteTopicEpic from './deleteTopicEpic';
import deleteTopicsEpic from './deleteTopicsEpic';
import updateTopicEpic from './updateTopicEpic';
import startTopicEpic from './startTopicEpic';
import startTopicsEpic from './startTopicsEpic';
import stopTopicEpic from './stopTopicEpic';
import stopTopicsEpic from './stopTopicsEpic';
import createAndStartTopicEpic from './createAndStartTopicEpic';
import stopAndDeleteTopicEpic from './stopAndDeleteTopicEpic';

export default combineEpics(
  fetchTopicsEpic,
  createTopicEpic,
  deleteTopicEpic,
  deleteTopicsEpic,
  updateTopicEpic,
  startTopicEpic,
  startTopicsEpic,
  stopTopicEpic,
  stopTopicsEpic,
  createAndStartTopicEpic,
  stopAndDeleteTopicEpic,
);
