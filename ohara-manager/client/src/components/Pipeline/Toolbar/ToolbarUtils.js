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

import * as connectorApi from 'api/connectorApi';
import * as streamApi from 'api/streamApi';

export const makeRequest = (pipeline, action) => {
  const { objects: services } = pipeline;

  const connectors = services.filter(
    service => service.kind === 'source' || service.kind === 'sink',
  );
  const streams = services.filter(service => service.kind === 'stream');

  let connectorPromises = [];
  let streamsPromises = [];

  if (action === 'start') {
    connectorPromises = connectors.map(({ group, name }) =>
      connectorApi.start({ group, name }),
    );
    streamsPromises = streams.map(({ group, name }) =>
      streamApi.start({ group, name }),
    );
  } else {
    connectorPromises = connectors.map(({ group, name }) =>
      connectorApi.stop({ group, name }),
    );
    streamsPromises = streams.map(({ group, name }) =>
      streamApi.stop({ group, name }),
    );
  }
  return Promise.all([...connectorPromises, ...streamsPromises]).then(
    result => result,
  );
};
