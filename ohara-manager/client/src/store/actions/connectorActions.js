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

export const fetchConnectors = createRoutine('FETCH_CONNECTORS');
export const fetchConnector = createRoutine('FETCH_CONNECTOR');
export const createConnector = createRoutine('CREATE_CONNECTOR');
export const updateConnector = createRoutine('UPDATE_CONNECTOR');
export const startConnector = createRoutine('START_CONNECTOR');
export const stopConnector = createRoutine('STOP_CONNECTOR');
export const deleteConnector = createRoutine('DELETE_CONNECTOR');
export const updateConnectorLink = createRoutine('UPDATE_CONNECTOR_LINK');
export const removeConnectorSourceLink = createRoutine(
  'REMOVE_CONNECTOR_SOURCE_LINK',
);
export const removeConnectorSinkLink = createRoutine(
  'REMOVE_CONNECTOR_SINK_LINK',
);
