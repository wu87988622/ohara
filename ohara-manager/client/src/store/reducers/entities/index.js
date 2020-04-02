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
import _ from 'lodash';
import brokers from './brokers';
import eventLogs from './eventLogs';
import files from './files';
import infos from './infos';
import nodes from './nodes';
import pipelines from './pipelines';
import topics from './topics';
import workers from './workers';
import workspaces from './workspaces';
import zookeepers from './zookeepers';

export const entity = type => (state = {}, action) => {
  const { payload } = action;
  if (payload && payload.entities) {
    return _.merge({}, state, payload.entities[type]);
  }
  return state;
};

export default combineReducers({
  brokers,
  eventLogs,
  files,
  infos,
  nodes,
  pipelines,
  topics,
  workers,
  workspaces,
  zookeepers,
});
