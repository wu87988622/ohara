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

import { has } from 'lodash';
import { SERVICE_STATE, ClusterData } from 'api/apiInterface/clusterInterface';

export function isServiceRunning(res: any) {
  return res?.data?.state === SERVICE_STATE.RUNNING;
}

export function isServiceStarted(data: ClusterData) {
  return data.state === SERVICE_STATE.RUNNING;
}

export function isServiceStopped(data: ClusterData) {
  return !has(data, 'state');
}
