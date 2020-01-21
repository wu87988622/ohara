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

export const initializeRoutine = createRoutine('INITIALIZE');
export const fetchConfiguratorRoutine = createRoutine('FETCH_CONFIGURATOR');
export const fetchZookeeperRoutine = createRoutine('FETCH_ZOOKEEPER');
export const fetchBrokerRoutine = createRoutine('FETCH_BROKER');
export const fetchWorkerRoutine = createRoutine('FETCH_WORKER');
export const fetchStreamRoutine = createRoutine('FETCH_STREAM');

export const setLogTypeRoutine = createRoutine('SET_LOG_TYPE');
export const setHostNameRoutine = createRoutine('SET_HOST_NAME');
export const setStreamNameRoutine = createRoutine('SET_STREAM_NAME');
export const setTimeGroupRoutine = createRoutine('SET_TIME_GROUP');
export const setTimeRangeRoutine = createRoutine('SET_TIME_RANGE');
export const setStartTimeRoutine = createRoutine('SET_START_TIME');
export const setEndTimeRoutine = createRoutine('SET_END_TIME');

export const refetchLogRoutine = createRoutine('REFETCH_LOG');
