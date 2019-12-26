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

const fetchWorkersRoutine = createRoutine('FETCH_WORKERS');
const createWorkerRoutine = createRoutine('CREATE_WORKER');
const updateWorkerRoutine = createRoutine('UPDATE_WORKER');
const stageWorkerRoutine = createRoutine('STAGE_WORKER');
const deleteWorkerRoutine = createRoutine('DELETE_WORKER');
const startWorkerRoutine = createRoutine('START_WORKER');
const stopWorkerRoutine = createRoutine('STOP_WORKER');

export {
  fetchWorkersRoutine,
  createWorkerRoutine,
  updateWorkerRoutine,
  stageWorkerRoutine,
  deleteWorkerRoutine,
  startWorkerRoutine,
  stopWorkerRoutine,
};
