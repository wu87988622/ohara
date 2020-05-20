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

import _ from 'lodash';
import { createSelector } from 'reselect';

const getEntities = state => state?.entities?.eventLogs;

export const makeGetEventLogs = () =>
  createSelector([getEntities], entities => entities);

export const makeGetNotifications = () =>
  createSelector([getEntities], entities => _.get(entities, 'notifications'));

export const makeGetSettings = () =>
  createSelector([getEntities], entities => _.get(entities, 'settings'));

export const isEventLogFetching = state =>
  !!_.get(state, ['ui', 'eventLog', 'isFetching']);
