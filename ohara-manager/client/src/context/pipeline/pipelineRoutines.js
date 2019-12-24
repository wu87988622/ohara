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

export const fetchPipelinesRoutine = createRoutine('FETCH_PIPELINES');
export const addPipelineRoutine = createRoutine('ADD_PIPELINE');
export const deletePipelineRoutine = createRoutine('DELETE_PIPELINE');
export const initializeRoutine = createRoutine('INITIALIZE');
export const setCurrentPipelineRoutine = createRoutine('SET_CURRENT_PIPELINE');
export const updatePipeineRoutine = createRoutine('UPDATE_PIPELINE');
