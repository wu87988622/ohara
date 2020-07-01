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

import { combineEpics } from 'redux-observable';
import createConnectorEpic from './createConnectorEpic';
import startConnectorEpic from './startConnectorEpic';
import stopConnectorEpic from './stopConnectorEpic';
import stopConnectorsEpic from './stopConnectorsEpic';
import deleteConnectorEpic from './deleteConnectorEpic';
import deleteConnectorsEpic from './deleteConnectorsEpic';
import updateConnectorEpic from './updateConnectorEpic';
import updateLinkConnectorEpic from './updateLinkConnectorEpic';
import removeSourceLinkEpic from './removeSourceLinkEpic';
import removeSinkLinkEpic from './removeSinkLinkEpic';
import fetchConnectorsEpic from './fetchConnectorsEpic';

export default combineEpics(
  createConnectorEpic,
  deleteConnectorEpic,
  deleteConnectorsEpic,
  fetchConnectorsEpic,
  removeSourceLinkEpic,
  removeSinkLinkEpic,
  startConnectorEpic,
  stopConnectorEpic,
  stopConnectorsEpic,
  updateConnectorEpic,
  updateLinkConnectorEpic,
);
