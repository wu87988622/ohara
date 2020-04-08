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

export const fetchDevToolTopicData = createRoutine('FETCH_DEV_TOOL_TOPIC_DATA');
export const setDevToolTopicQueryParams = createRoutine(
  'SET_DEV_TOOL_TOPIC_QUERY_PARAMS',
);

export const fetchDevToolLog = createRoutine('FETCH_DEV_TOOL_LOG');
export const setDevToolLogQueryParams = createRoutine(
  'SET_DEV_TOOL_LOG_QUERY_PARAMS',
);
