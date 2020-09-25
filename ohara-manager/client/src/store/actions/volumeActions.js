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

export const fetchVolumes = createRoutine('FETCH_VOLUMES');
export const fetchVolume = createRoutine('FETCH_VOLUME');
export const createVolume = createRoutine('CREATE_VOLUME');
export const updateVolume = createRoutine('UPDATE_VOLUME');
export const startVolume = createRoutine('START_VOLUME');
export const stopVolume = createRoutine('STOP_VOLUME');
export const deleteVolume = createRoutine('DELETE_VOLUME');
export const inspectVolume = createRoutine('INSPECT_VOLUME');
export const validateVolumePath = createRoutine('VALIDATE_VOLUME_PATH');
export const clearValidate = createRoutine('CLEAR_VALIDATE');
