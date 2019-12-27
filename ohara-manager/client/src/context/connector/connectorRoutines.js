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

const fetchConnectorsRoutine = createRoutine('FETCH_CONNECTORS');
const createConnectorRoutine = createRoutine('CREATE_CONNECTOR');
const updateConnectorRoutine = createRoutine('UPDATE_CONNECTOR');
const stageConnectorRoutine = createRoutine('STAGE_CONNECTOR');
const deleteConnectorRoutine = createRoutine('DELETE_CONNECTOR');
const startConnectorRoutine = createRoutine('START_CONNECTOR');
const stopConnectorRoutine = createRoutine('STOP_CONNECTOR');

export {
  fetchConnectorsRoutine,
  createConnectorRoutine,
  updateConnectorRoutine,
  stageConnectorRoutine,
  deleteConnectorRoutine,
  startConnectorRoutine,
  stopConnectorRoutine,
};
