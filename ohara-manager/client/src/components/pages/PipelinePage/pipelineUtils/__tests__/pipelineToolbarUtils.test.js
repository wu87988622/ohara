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

import { CONNECTOR_TYPES } from 'constants/pipelines';
import * as generate from 'utils/generate';
import * as connectorApi from 'api/connectorApi';

import { createConnector, trimString } from '../pipelineToolbarUtils';

jest.mock('api/connectorApi');
jest.mock('api/streamApi');

describe('createConnector()', () => {
  const updateGraph = jest.fn();

  afterEach(jest.clearAllMocks);

  it('updates a topic', async () => {
    const topicId = generate.id();
    const topicName = generate.name();

    const connector = {
      className: CONNECTOR_TYPES.topic,
      typeName: CONNECTOR_TYPES.topic,
      id: topicId,
      name: topicName,
    };

    await createConnector({ updateGraph, connector });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith({
      update: {
        name: topicName,
        kind: CONNECTOR_TYPES.topic,
        to: [],
        className: CONNECTOR_TYPES.topic,
        id: topicId,
      },
    });
  });

  it('updates a custom connector', async () => {
    const connectorId = 'testConnector';
    const customConnectorClassName = generate.name();
    const typeName = 'source';
    const connectorName = 'testConnector';

    const connector = {
      className: customConnectorClassName,
      typeName,
    };
    const newConnectorName = connectorName;
    const res = {
      data: { result: { id: connectorId, settings: { name: connectorName } } },
    };

    connectorApi.createConnector.mockImplementation(() => Promise.resolve(res));

    await createConnector({ updateGraph, connector, newConnectorName });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith({
      update: {
        name: connectorName,
        kind: typeName,
        to: [],
        className: customConnectorClassName,
        id: connectorId,
      },
    });
  });
});

describe('trimString()', () => {
  it('gets the correct string', () => {
    expect(trimString(generate.id()).length).toBe(7);
  });
});
