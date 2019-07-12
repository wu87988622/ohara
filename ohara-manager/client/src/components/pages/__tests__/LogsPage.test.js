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
import { cleanup, render, waitForElement } from '@testing-library/react';
import 'jest-dom/extend-expect';

import * as generate from 'utils/generate';
import LogsPage from '../LogsPage';
import { LOGS, NOT_FOUND_PAGE } from 'constants/documentTitles';
import { fetchLogs } from 'api/logApi';

jest.mock('api/logApi');

const props = {
  match: { params: jest.fn() },
};

afterEach(cleanup);

describe('<LogsPage />', () => {
  const match = {
    params: {
      serviceName: 'workers',
      clusterName: generate.name(),
    },
  };

  const logs = [{ value: generate.message() }, { value: generate.message() }];

  beforeEach(() => {
    const res = {
      data: {
        result: {
          logs,
        },
      },
    };
    fetchLogs.mockImplementation(() => Promise.resolve(res));
  });

  it('renders the page', () => {
    render(<LogsPage {...props} match={match} />);
  });

  it('renders the right document title', () => {
    render(<LogsPage {...props} match={match} />);
    expect(document.title).toBe(LOGS);
  });

  it('renders page title', async () => {
    const { getByText } = render(<LogsPage {...props} match={match} />);

    getByText('Error log of cluster ' + match.params.clusterName);
  });

  it('renders the loading indicator', () => {
    const { getByTestId } = render(<LogsPage {...props} match={match} />);

    getByTestId('table-loader');
  });

  it('renders log content', async () => {
    const { getByText } = await waitForElement(() =>
      render(<LogsPage {...props} match={match} />),
    );

    getByText(logs[0].value);
  });

  it('redirects to not found page when a wrong service name is given', async () => {
    const match = {
      params: {
        serviceName: 'a wrong service',
        clusterName: generate.name(),
      },
    };

    const { getByText } = await waitForElement(() =>
      render(<LogsPage {...props} match={match} />),
    );

    expect(document.title).toBe(NOT_FOUND_PAGE);
    getByText('Oops, page not found');
  });

  it('renders the page even if the API returns empty logs', async () => {
    const res = {
      data: {
        result: {
          logs: [],
        },
      },
    };

    fetchLogs.mockImplementation(() => Promise.resolve(res));

    const match = {
      params: {
        serviceName: 'workers',
        clusterName: generate.name(),
      },
    };

    const { queryByText, getByText } = await waitForElement(() =>
      render(<LogsPage {...props} match={match} />),
    );

    getByText('Error log of cluster ' + match.params.clusterName);
    expect(queryByText(logs[0].value)).toBeNull();
  });
});
