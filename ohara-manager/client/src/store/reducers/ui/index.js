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
import app from './app';
import brokers from './brokers';
import connector from './connector';
import createWorkspace from './createWorkspace';
import deleteWorkspace from './deleteWorkspace';
import devTool from './devTool';
import eventLog from './eventLog';
import file from './file';
import intro from './intro';
import logProgress from './logProgress';
import node from './node';
import pipeline from './pipeline';
import restartWorkspace from './restartWorkspace';
import shabondi from './shabondi';
import stream from './stream';
import snackbar from './snackbar';
import topic from './topic';
import workers from './workers';
import workspace from './workspace';
import zookeepers from './zookeepers';

export default combineReducers({
  app,
  brokers,
  connector,
  createWorkspace,
  deleteWorkspace,
  devTool,
  eventLog,
  file,
  intro,
  logProgress,
  node,
  pipeline,
  restartWorkspace,
  shabondi,
  stream,
  snackbar,
  topic,
  workers,
  workspace,
  zookeepers,
});
