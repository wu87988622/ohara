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
import { cleanup, waitForElement, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import PipelineListPage from '../PipelineListPage';
import { PIPELINE } from 'constants/documentTitles';
import { renderWithProvider } from 'utils/testUtils';
import * as generate from 'utils/generate';
import * as URLS from 'constants/urls';
import * as useApi from 'components/controller';
import * as API_URL from 'components/controller/url';

jest.mock('api/infoApi');
jest.mock('components/controller');

const props = {
  match: {
    url: '/to/a/new/page',
  },
  history: { push: jest.fn() },
};

jest.doMock('../PipelineToolbar', () => {
  const PipelineToolbar = () => <div />;
  return PipelineToolbar;
});

afterEach(cleanup);

const workers = generate.workers({ count: 1 });
const pipelines = [
  {
    name: 'a',
    workerClusterName: 'worker-a',
    status: 'Stopped',
    id: '1234',
    objects: [
      { kind: 'topic', id: '123', name: 'bb' },
      { kind: 'source', id: '789', name: 'dd' },
    ],
  },
  {
    name: 'b',
    workerClusterName: 'worker-b',
    status: 'Running',
    id: '5678',
    objects: [
      { kind: 'topic', id: '456', name: 'aa' },
      { kind: 'source', id: '012', name: 'cc' },
    ],
  },
];

describe('<PipelineListPage />', () => {
  beforeEach(() => {
    jest.spyOn(useApi, 'usePostApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useDeleteApi').mockImplementation(() => {
      return {
        getData: jest.fn(),
      };
    });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
      if (url === `${API_URL.WORKER_URL}`) {
        return {
          data: {
            data: {
              result: [workers],
            },
          },
          isLoading: false,
          refetch: jest.fn(),
        };
      }
      if (url === API_URL.PIPELINE_URL) {
        return {
          data: {
            data: {
              isSuccess: true,
              result: pipelines,
            },
          },
          isLoading: false,
          refetch: jest.fn(),
        };
      }
    });
  });

  it('renders page title', async () => {
    renderWithProvider(<PipelineListPage {...props} />);
    expect(document.title).toBe(PIPELINE);
  });

  it('renders page heading', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );
    getByText('Pipelines');
  });

  it('renders new pipeline button', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );
    getByText('New pipeline');
  });

  it('toggles new pipeline modal', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );
    expect(queryByTestId('new-pipeline-modal')).toBeNull();
    fireEvent.click(getByTestId('new-pipeline'));

    expect(queryByTestId('new-pipeline-modal')).toBeVisible();

    fireEvent.click(getByText('Cancel'));
    expect(queryByTestId('new-pipeline-modal')).not.toBeVisible();
  });

  it(`renders new pipeline modal's content`, async () => {
    const { getByText, getAllByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('new-pipeline'));

    // Modal title
    getAllByText('New pipeline');

    // label
    getByText('Pipeline name');

    // select
    getByText('Workspace name');
    getByTestId('cluster-select');

    expect(getByText('Add')).toBeDisabled();
  });

  it('renders pipeline data list', async () => {
    const { getByText, getAllByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    getByText('Name');
    getByText('Workspace');
    getByText('Status');
    getByText('Edit');
    getByText('Delete');

    const pipelineTr = await waitForElement(() =>
      getAllByTestId('pipeline-name'),
    );

    expect(pipelineTr.length).toBe(pipelines.length);
  });

  it('toggles delete pipeline modal', async () => {
    const pipelineName = generate.name();

    const pipelines = [
      {
        name: pipelineName,
        workerClusterName: 'worker-a',
        status: 'Stopped',
        id: '1234',
        objects: [
          { kind: 'topic', id: '123', name: 'bb' },
          { kind: 'source', id: '789', name: 'dd' },
        ],
      },
    ];

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
      if (url === `${API_URL.WORKER_URL}`) {
        return {
          data: {
            data: {
              result: [workers],
            },
          },
          isLoading: false,
          refetch: jest.fn(),
        };
      }
      if (url === API_URL.PIPELINE_URL) {
        return {
          data: {
            data: {
              isSuccess: true,
              result: pipelines,
            },
          },
          isLoading: false,
          refetch: jest.fn(),
        };
      }
    });

    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('delete-pipeline'));

    getByTestId('delete-dialog');
    getByText('Delete pipeline?');
    getByText(
      `Are you sure you want to delete the pipeline: ${pipelineName}? This action cannot be undone!`,
    );

    expect(getByTestId('delete-dialog')).toBeVisible();

    fireEvent.click(getByText('Cancel'));
    expect(getByTestId('delete-dialog')).not.toBeVisible();
  });

  it(`displays a redirect message when there's no workspace in the dropdown`, async () => {
    jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
      if (url === `${API_URL.WORKER_URL}`) {
        return {
          data: {},
          isLoading: false,
          refetch: jest.fn(),
        };
      }
      if (url === API_URL.PIPELINE_URL) {
        return {
          data: {
            data: {
              isSuccess: true,
              result: pipelines,
            },
          },
          isLoading: false,
          refetch: jest.fn(),
        };
      }
    });

    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('new-pipeline'));

    expect(getByTestId('warning-message').textContent).toBe(
      "It seems like you haven't created any worker clusters yet. You can create one from here",
    );

    const expectedUrl = generate.serverHost() + URLS.WORKSPACES;
    expect(getByText('here').href).toBe(expectedUrl);
  });
});
