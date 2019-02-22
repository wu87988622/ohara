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
import * as connectorApi from 'api/connectorApi';

import { createConnector } from '../pipelineToolbarUtils';

jest.mock('api/connectorApi');

describe('createConnector()', () => {
  it('should call updateGraph function if the given type is not exist in the current graph', async () => {
    const graph = [{ name: 'a', type: CONNECTOR_TYPES.topic }];
    const updateGraph = jest.fn();
    const connector = { className: CONNECTOR_TYPES.ftpSource };
    const res = { data: { result: { id: '1234' } } };

    connectorApi.createConnector.mockImplementation(() => Promise.resolve(res));

    await createConnector({ graph, updateGraph, connector });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith({
      update: {
        icon: 'fa-file-import',
        isActive: false,
        name: expect.any(String),
        to: [],
        kind: CONNECTOR_TYPES.ftpSource,
        id: res.data.result.id,
      },
    });
  });
});
