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

const fetchBrokersRoutine = createRoutine('FETCH_BROKERS');
const addBrokerRoutine = createRoutine('ADD_BROKER');
const updateBrokerRoutine = createRoutine('UPDATE_BROKER');
const stageBrokerRoutine = createRoutine('STAGE_BROKER');
const deleteBrokerRoutine = createRoutine('DELETE_BROKER');

export {
  fetchBrokersRoutine,
  addBrokerRoutine,
  updateBrokerRoutine,
  stageBrokerRoutine,
  deleteBrokerRoutine,
};
