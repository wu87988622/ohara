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
    workerClusterName: generate.serviceName(),
  },
  updateGraph: jest.fn(),
  refreshGraph: jest.fn(),
  updateHasChanges: jest.fn(),
  pipelineTopics: generate.topics(),
  history: {
    push: jest.fn(),
  },
};

// Skip the tests for now
describe.skip('<StreamApp />', () => {
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
          id: generate.id(),
          from: [],
          to: [],
          instances: 0,
          name: generate.name(),
          jar: {
            group: generate.name(),
            name: generate.name(),
          },
        },
      },
    };

    fetchProperty.mockImplementation(() => Promise.resolve(res));

    const { getByText, getByLabelText, getByTestId } = await render(
      <StreamApp {...props} />,
    );

    const { instances, jar } = res.data.result;
    const { name: jarName } = jar;

    expect(getByText('Stream app')).toBeInTheDocument();
    expect(getByLabelText('Instances')).toHaveAttribute(
      'value',
      String(instances),
    );

    // TODO: add select tests

    expect(getByTestId('start-button')).toBeInTheDocument();
    expect(getByTestId('stop-button')).toBeInTheDocument();
    expect(getByText(jarName)).toBeInTheDocument();
  });
});
