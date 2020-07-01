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

export const createWorkspace = createRoutine('CREATE_WORKSPACE');
export const deleteWorkspace = createRoutine('DELETE_WORKSPACE');
export const fetchWorkspaces = createRoutine('FETCH_WORKSPACES');
export const updateWorkspace = createRoutine('UPDATE_WORKSPACE');
export const switchWorkspace = createRoutine('SWITCH_WORKSPACE');
export const restartWorkspace = createRoutine('RESTART_WORKSPACE');
