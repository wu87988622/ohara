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

export const fetchBrokers = createRoutine('FETCH_BROKERS');
export const fetchBroker = createRoutine('FETCH_BROKER');
export const createBroker = createRoutine('CREATE_BROKER');
export const updateBroker = createRoutine('UPDATE_BROKER');
export const startBroker = createRoutine('START_BROKER');
export const stopBroker = createRoutine('STOP_BROKER');
export const deleteBroker = createRoutine('DELETE_BROKER');
