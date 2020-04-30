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

export const openRestartWorkspace = createRoutine('OPEN_RESTART_WORKSPACE');
export const closeRestartWorkspace = createRoutine('CLOSE_RESTART_WORKSPACE');
export const pauseRestartWorkspace = createRoutine('PAUSE_RESTART_WORKSPACE');
export const resumeRestartWorkspace = createRoutine('RESUME_RESTART_WORKSPACE');
export const rollbackRestartWorkspace = createRoutine(
  'ROLLBACK_RESTART_WORKSPACE',
);
export const autoCloseRestartWorkspace = createRoutine(
  'AUTO_CLOSE_RESTART_WORKSPACE',
);
