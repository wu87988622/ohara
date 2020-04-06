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
import { getId } from 'utils/object';

const getEntities = state => state?.entities?.workers;
const getIdFromProps = (_, props) => props?.id;
const getNameFromProps = (_, props) => props?.name;

export const makeGetAllWorkers = () =>
  createSelector([getEntities], entities => _.values(entities));

export const makeGetWorkerById = () =>
  createSelector([getEntities, getIdFromProps], (entities, id) => entities[id]);

export const makeGetWorkerByName = () =>
  createSelector(
    [getEntities, getNameFromProps],
    (entities, name) => entities[getId({ group: 'worker', name })],
  );
