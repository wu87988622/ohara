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

import createRoutine from './createRoutine';

export const fetchWorkers = createRoutine('FETCH_WORKERS');
export const fetchWorker = createRoutine('FETCH_WORKER');
export const createWorker = createRoutine('CREATE_WORKER');
export const updateWorker = createRoutine('UPDATE_WORKER');
export const startWorker = createRoutine('START_WORKER');
export const stopWorker = createRoutine('STOP_WORKER');
export const deleteWorker = createRoutine('DELETE_WORKER');
export const inspectWorker = createRoutine('INSPECT_WORKER');
