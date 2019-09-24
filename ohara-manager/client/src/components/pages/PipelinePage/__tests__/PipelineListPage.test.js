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
import { waitForElement, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import PipelineListPage from '../PipelineListPage/PipelineListPage';
import { PIPELINE } from 'constants/documentTitles';
import { renderWithProvider } from 'utils/testUtils';
import { fetchWorkers } from 'api/workerApi';
import { fetchPipelines } from 'api/pipelineApi';
import * as generate from 'utils/generate';
import * as URLS from 'constants/urls';
import * as useApi from 'components/controller';

jest.mock('api/workerApi');
jest.mock('api/pipelineApi');
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

describe('<PipelineListPage />', () => {
  let pipelines;
  beforeEach(() => {
    const workers = generate.workers({ count: 1 });

    pipelines = [
      {
        name: generate.name(),
        status: 'Stopped',
        objects: [
          { kind: 'topic', name: 'bb' },
          { kind: 'source', name: 'dd' },
        ],
        tags: { workerClusterName: generate.serviceName() },
      },
    ];

    fetchWorkers.mockImplementation(() =>
      Promise.resolve({
        data: {
          result: workers,
        },
      }),
    );

    fetchPipelines.mockImplementation(() =>
      Promise.resolve({
        data: {
          result: pipelines,
        },
      }),
    );

    jest.spyOn(useApi, 'useWaitApi').mockImplementation(() => {
      return { putApi: jest.fn() };
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
    getByText('NEW PIPELINE');
  });

  it('toggles new pipeline modal', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );
    expect(queryByTestId('new-pipeline-modal')).toBeNull();
    fireEvent.click(getByTestId('new-pipeline'));

    expect(queryByTestId('new-pipeline-modal')).toBeVisible();

    fireEvent.click(getByText('CANCEL'));
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

    // dropdown
    getByText('Workspace name');
    getByTestId('workspace-name-select');

    expect(getByText('ADD')).toBeDisabled();
  });

  it('renders pipeline data list', async () => {
    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    // Ensure we're rendering the correct table head
    getByText('Name');
    getByText('Workspace');
    getByText('Status');
    getByText('Edit');
    getByText('Delete');

    const [pipeline] = pipelines;

    expect(getByTestId('pipeline-name').textContent).toBe(pipeline.name);
    expect(getByTestId('pipeline-workspace').textContent).toBe(
      pipeline.tags.workerClusterName,
    );
    expect(getByTestId('pipeline-status').textContent).toBe(pipeline.status);
  });

  it('toggles delete pipeline modal', async () => {
    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<PipelineListPage {...props} />),
    );

    fireEvent.click(getByTestId('delete-pipeline'));

    getByTestId('delete-dialog');
    getByText('Delete pipeline?');
    getByText(
      `Are you sure you want to delete the pipeline: ${pipelines[0].name}? This action cannot be undone!`,
    );

    expect(getByTestId('delete-dialog')).toBeVisible();

    fireEvent.click(getByText('CANCEL'));
    expect(getByTestId('delete-dialog')).not.toBeVisible();
  });

  it(`displays a redirect message when there's no workspace in the dropdown`, async () => {
    fetchWorkers.mockImplementation(() =>
      Promise.resolve({
        data: {
          result: [],
        },
      }),
    );

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
