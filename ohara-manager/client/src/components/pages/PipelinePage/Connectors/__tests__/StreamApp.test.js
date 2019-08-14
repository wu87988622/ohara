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

import React from 'react';
import { render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import StreamApp from '../StreamApp';
import { fetchProperty } from 'api/streamApi';

jest.mock('api/streamApi');

const props = {
  match: {
    params: {
      connectorName: generate.serviceName(),
    },
  },
  graph: [],
  pipeline: {
    name: generate.name(),
    flows: [],
    tags: { workerClusterName: generate.serviceName() },
  },
  updateGraph: jest.fn(),
  refreshGraph: jest.fn(),
  updateHasChanges: jest.fn(),
  pipelineTopics: generate.topics(),
  history: {
    push: jest.fn(),
  },
};

describe('<StreamApp />', () => {
  it('renders without crash', () => {
    render(<StreamApp {...props} />);
  });

  it('renders nothing when data is not ready', () => {
    const { queryByText } = render(<StreamApp {...props} />);
    expect(queryByText('Stream app')).toBe(null);
  });

  it('renders components', async () => {
    const res = {
      data: {
        result: {
          definition: {
            className: 'com.island.ohara.it.streamapp.DumbStreamApp',
            definitions: [
              {
                reference: 'NONE',
                displayName: 'Author',
                internal: false,
                documentation: 'Author of streamApp',
                valueType: 'STRING',
                tableKeys: [],
                orderInGroup: 14,
                key: 'author',
                required: false,
                defaultValue: 'root',
                group: 'core',
                editable: false,
              },
              {
                reference: 'TOPIC',
                displayName: 'From topic of data consuming from',
                internal: false,
                documentation:
                  'The topic name of this streamApp should consume from',
                valueType: 'STRING',
                tableKeys: [],
                orderInGroup: 6,
                key: 'from',
                required: true,
                defaultValue: null,
                group: 'core',
                editable: true,
              },
              {
                reference: 'NONE',
                displayName: 'Instances',
                internal: false,
                documentation: 'The running container number of this streamApp',
                valueType: 'INT',
                tableKeys: [],
                orderInGroup: 9,
                key: 'instances',
                required: true,
                defaultValue: null,
                group: 'core',
                editable: true,
              },
              {
                reference: 'NONE',
                displayName: 'StreamApp name',
                internal: false,
                documentation: 'The unique name of this streamApp',
                valueType: 'STRING',
                tableKeys: [],
                orderInGroup: 2,
                key: 'name',
                required: false,
                defaultValue: null,
                group: 'core',
                editable: true,
              },
            ],
          },
          settings: {
            from: [],
            to: [],
            instances: 1,
            name: generate.name(),
            jar: {
              group: generate.name(),
              name: generate.name(),
            },
          },
        },
      },
    };

    fetchProperty.mockImplementation(() => Promise.resolve(res));

    const { getByText, getByLabelText, getByTestId } = await render(
      <StreamApp {...props} />,
    );

    const { instances } = res.data.result.settings;

    getByText('Stream app');
    expect(getByLabelText('Instances')).toHaveAttribute(
      'value',
      String(instances),
    );

    expect(getByTestId('start-button')).toBeInTheDocument();
    expect(getByTestId('stop-button')).toBeInTheDocument();
  });
});
