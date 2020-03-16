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

const BASE_URL = '/api';

// Clusters
export const ZOOKEEPER_URL = `${BASE_URL}/zookeepers`;
export const BROKER_URL = `${BASE_URL}/brokers`;
export const WORKER_URL = `${BASE_URL}/workers`;
export const STREAM_URL = `${BASE_URL}/streams`;

// Others
export const CONNECTOR_URL = `${BASE_URL}/connectors`;
export const NODE_URL = `${BASE_URL}/nodes`;
export const PIPELINE_URL = `${BASE_URL}/pipelines`;
export const TOPIC_URL = `${BASE_URL}/topics`;
export const LOG_URL = `${BASE_URL}/logs`;
export const VALIDATE_URL = `${BASE_URL}/validate`;
export const CONTAINER_URL = `${BASE_URL}/containers`;
export const INSPECT_URL = `${BASE_URL}/inspect`;
export const FILE_URL = `${BASE_URL}/files`;
export const OBJECT_URL = `${BASE_URL}/objects`;

// Helper function
export const toQueryParameters = (params = {}) => {
  if (typeof params !== 'object') {
    throw new Error('you need to pass an object');
  }
  const esc = encodeURIComponent;
  const result = Object.keys(params)
    // we explicitly filter out the "name" field
    // since it is used in url path
    .filter(key => key !== 'name')
    .map(key => `${key}=${esc(params[key])}`)
    .join('&');
  return result ? '?'.concat(result) : '';
};
