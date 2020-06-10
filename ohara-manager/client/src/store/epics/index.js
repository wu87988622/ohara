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

import appEpics from './app';
import brokerEpics from './broker';
import devToolEpics from './devTool';
import connectorEpics from './connector';
import eventLogEpics from './eventLog';
import fileEpics from './file';
import logProgressEpics from './logProgress';
import nodeEpics from './nodeEpics';
import pipelineEpics from './pipeline';
import shabondiEpics from './shabondi';
import streamEpics from './stream';
import snackbarEpics from './snackbarEpics';
import topicEpics from './topic';
import workerEpics from './worker';
import workspaceEpics from './workspace';
import zookeeperEpics from './zookeeper';

export default combineEpics(
  appEpics,
  brokerEpics,
  devToolEpics,
  connectorEpics,
  eventLogEpics,
  fileEpics,
  logProgressEpics,
  nodeEpics,
  shabondiEpics,
  streamEpics,
  pipelineEpics,
  snackbarEpics,
  topicEpics,
  workerEpics,
  workspaceEpics,
  zookeeperEpics,
);
