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
import { renderWithRouter, renderWithProvider } from 'utils/testUtils';
import { fetchWorkers } from 'api/workerApi';
import { fetchPipelines } from 'api/pipelineApi';
import * as generate from 'utils/generate';
import * as URLS from 'constants/urls';

jest.mock('api/infoApi');
jest.mock('api/workerApi');
jest.mock('api/pipelineApi');
jest.mock('api/workerApi');

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
    const resWorker = {
      data: {
        result: workers,
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(resWorker));

    const resPipelines = {
      data: {
        result: pipelines,
      },
    };

    fetchPipelines.mockImplementation(() => Promise.resolve(resPipelines));
  });

  it('renders page title', async () => {
    renderWithRouter(<PipelineListPage {...props} />);
    expect(document.title).toBe(PIPELINE);
  });

  it('renders page heading', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );
    getByText('Pipelines');
  });

  it('renders a loading indicator when the pipeline data is still loading', async () => {
    const { getByTestId } = renderWithRouter(<PipelineListPage {...props} />);
    getByTestId('table-loader');
  });

  it('renders new pipeline button', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );
    getByText('New pipeline');
  });

  it('toggles new pipeline modal', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );
    expect(queryByTestId('new-pipeline-modal')).toBeNull();
    fireEvent.click(getByTestId('new-pipeline'));

    expect(queryByTestId('new-pipeline-modal')).not.toBeNull();

    fireEvent.click(getByText('Cancel'));
    expect(queryByTestId('new-pipeline-modal')).toBeNull();
  });

  it(`renders new pipeline modal's content`, async () => {
    const {
      getByText,
      getAllByText,
      getByTestId,
      getByLabelText,
    } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('new-pipeline'));

    // Modal title
    getAllByText('New pipeline');

    // label
    getByText('Pipeline name');
    getByLabelText('Pipeline name');

    // select
    getByText('Workspace name');
    getByTestId('cluster-select');

    expect(getByTestId('cluster-select').length).toBe(workers.length);

    expect(getByText('Add')).not.toBeDisabled();
  });

  it(`displays a redirect message when there's no workspace in the dropdown`, async () => {
    const workerResponse = {
      data: {
        result: [],
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(workerResponse));

    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('new-pipeline'));

    // If there's no workspace in the select
    expect(queryByTestId('cluster-select')).toBeNull();

    expect(getByTestId('warning-message').textContent).toBe(
      "It seems like you haven't created any worker clusters yet. You can create one from here",
    );

    const expectedUrl = generate.serverHost() + URLS.WORKSPACES;
    expect(getByText('here').href).toBe(expectedUrl);

    expect(getByText('Add')).toBeDisabled();
  });

  it('renders pipeline data list', async () => {
    const { getByText, getAllByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );

    getByText('name');
    getByText('workspace');
    getByText('status');
    getByText('edit');
    getByText('delete');

    const pipelineTr = await waitForElement(() =>
      getAllByTestId('pipeline-tr'),
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

    const pipelineResponse = {
      data: {
        result: pipelines,
      },
    };

    fetchPipelines.mockImplementation(() => Promise.resolve(pipelineResponse));

    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('delete-pipeline-btn'));

    getByTestId('delete-dialog');
    getByText('Delete pipeline?');
    getByText(
      `Are you sure you want to delete the pipeline: ${pipelineName}? This action cannot be undone!`,
    );

    expect(getByTestId('delete-dialog')).toBeVisible();

    fireEvent.click(getByText('Cancel'));
    expect(getByTestId('delete-dialog')).not.toBeVisible();
  });

  it('renders new pipeline modal with multiple workspace data', async () => {
    const workers = generate.workers({ count: 3 });

    const workerResponse = {
      data: {
        result: workers,
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(workerResponse));

    const { getByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('new-pipeline'));
    expect(getByTestId('cluster-select').length).toBe(workers.length);

    fireEvent.change(getByTestId('cluster-select'), {
      target: { value: workers[1].name },
    });

    expect(getByTestId('cluster-select').value).toBe(workers[1].name);
  });
});
