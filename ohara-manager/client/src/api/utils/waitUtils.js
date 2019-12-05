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

import { isEmpty } from 'lodash';

export const waitForConnectReady = res => {
  const { classInfos } = res.data.result;
  return !isEmpty(classInfos);
};

export const waitForRunning = res => {
  const { state } = res.data.result;
  return state === 'RUNNING';
};

export const waitForStop = res => {
  const { state } = res.data.result;
  return state === undefined;
};

export const waitForConnectRunning = res => {
  const { status } = res.data.result;
  return status.state === 'RUNNING';
};

export const waitForConnectStop = res => {
  const { status } = res.data.result;
  return status === undefined;
};

export const waitForClusterNonexistent = (res, params) => {
  const { name, group } = params;
  const result = res.data.result
    .map(res => res.settings)
    .some(settings => settings.name === name && settings.group === group);
  return !result;
};

export const waitForNodeNonexistent = (res, params) => {
  const { hostname } = params;
  const result = res.data.result.some(node => node.hostname === hostname);
  return !result;
};

export const waitForObjectNonexistent = (res, params) => {
  const { name, group } = params;
  const result = res.data.result.some(
    d => d.name === name && d.group === group,
  );
  return !result;
};

export const waitForNodeReady = (res, node) => {
  const nodeNames = res.data.result.settings.nodeNames;
  return nodeNames.includes(node);
};

export const waitForNodeNonexistentInCluster = (res, node) => {
  const nodeNames = res.data.result.settings.nodeNames;
  return !nodeNames.includes(node);
};
