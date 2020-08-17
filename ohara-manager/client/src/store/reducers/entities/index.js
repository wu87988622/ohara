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

import { combineReducers } from 'redux';
import { isArray, mergeWith, isEmpty } from 'lodash';
import brokers from './brokers';
import connectors from './connectors';
import eventLogs from './eventLogs';
import files from './files';
import infos from './infos';
import nodes from './nodes';
import pipelines from './pipelines';
import shabondis from './shabondis';
import streams from './streams';
import topics from './topics';
import volumes from './volumes';
import workers from './workers';
import workspaces from './workspaces';
import zookeepers from './zookeepers';
import { getId } from 'utils/object';

// Allow using an empty array to override previous state
const customizer = (object, source) =>
  isArray(source) && isEmpty(source) ? source : object;

export const entity = (type) => (state = {}, action) => {
  const { payload } = action;

  const hasEntities = payload?.entities || payload?.[0]?.entities;

  if (hasEntities) {
    if (isArray(payload)) {
      const [result] = payload.map((p) =>
        mergeWith(p.entities[type], state, customizer),
      );
      return result;
    }
    return mergeWith(payload.entities[type], state, customizer);
  }

  return state;
};

export const deleteEntitiesByIds = (state, action) => {
  return Object.values(state).reduce((acc, entity) => {
    if (!action.payload.includes(getId(entity))) {
      acc[getId(entity)] = entity;
    }
    return acc;
  }, {});
};

export default combineReducers({
  brokers,
  connectors,
  eventLogs,
  files,
  infos,
  nodes,
  pipelines,
  shabondis,
  streams,
  topics,
  volumes,
  workers,
  workspaces,
  zookeepers,
});
