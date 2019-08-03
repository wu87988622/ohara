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

import * as generate from 'utils/generate';
import * as pipelineApi from 'api/pipelineApi';
import * as connectorApi from 'api/connectorApi';
import * as workerApi from 'api/workerApi';
import * as topicApi from 'api/topicApi';
import PipelineNewPage from '../PipelineNewPage';
import { PIPELINE_EDIT } from 'constants/documentTitles';
import { renderWithRouter } from 'utils/testUtils';

jest.mock('api/pipelineApi');
jest.mock('api/connectorApi');
jest.mock('api/workerApi');
jest.mock('api/topicApi');

const props = {
  match: { params: jest.fn() },
};

const workerResponse = {
  result: {
    connectors: generate.connectors(),
  },
};

workerApi.fetchWorker.mockImplementation(() =>
  Promise.resolve({ data: workerResponse }),
);

const pipeline = {
  name: generate.name(),
  workerClusterName: generate.name(),
  status: 'Running',
  id: '1234',
  objects: [
    { kind: 'topic', id: '123', name: generate.name(), state: true },
    { kind: 'source', id: '789', name: generate.name(), state: true },
  ],
  rules: {},
};

const topics = generate.topics({ count: 1 });

afterEach(cleanup);

describe('<PipelineNewPage />', () => {
  const match = {
    params: {
      pipelineName: generate.name(),
      connectorName: generate.name(),
    },
  };

  beforeEach(() => {
    const resPipelines = {
      data: {
        result: pipeline,
      },
    };

    pipelineApi.fetchPipeline.mockImplementation(() =>
      Promise.resolve(resPipelines),
    );

    const topicResponse = {
      data: {
        result: topics,
      },
    };

    topicApi.fetchTopics.mockImplementation(() =>
      Promise.resolve(topicResponse),
    );
  });

  it('renders self', async () => {
    await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
  });

  it('renders edit pipeline page document title, if pipelineName is present', async () => {
    await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
    expect(document.title).toBe(PIPELINE_EDIT);
  });

  it('display the pipeline name', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
    getByText(pipeline.name);
  });

  it('renders Toolbar', async () => {
    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
    getByTestId('toolbar-sources');
    getByTestId('toolbar-topics');
    getByTestId('toolbar-streams');
    getByTestId('toolbar-sinks');
    getByText('All changes saved');
  });

  it('renders Pipeline graph title', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
    getByText('Pipeline graph');
  });

  it('renders Pipeline operate', async () => {
    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );
    getByText('Operate');
    getByTestId('start-btn');
    getByTestId('stop-btn');
    getByText('This pipeline is running on: ' + pipeline.workerClusterName);
  });

  it('starts the pipeline', async () => {
    const data = {
      result: {
        name: 'test',
        status: 'Stopped',
        objects: [
          { kind: 'source', name: 'c', id: '3' },
          { kind: 'sink', name: 'b', id: '2' },
          { kind: 'topic', name: 'a', id: '1' },
        ],
        rules: {},
        workerClusterName: generate.name(),
      },
    };

    pipelineApi.fetchPipeline.mockImplementation(() =>
      Promise.resolve({ data }),
    );

    connectorApi.startConnector.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    const { getByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );

    // Start the pipeline
    fireEvent.click(await waitForElement(() => getByTestId('start-btn')));

    expect(connectorApi.startConnector).toHaveBeenCalledTimes(2);
    expect(connectorApi.startConnector).toHaveBeenCalledWith(
      data.result.objects[0].name,
    );
    expect(connectorApi.startConnector).toHaveBeenCalledWith(
      data.result.objects[1].name,
    );
  });

  it('stops the pipeline', async () => {
    const data = {
      result: {
        name: 'test',
        objects: [
          { kind: 'source', name: 'c', id: '3' },
          { kind: 'sink', name: 'b', id: '2' },
          { kind: 'topic', name: 'a', id: '1' },
        ],
        rules: {},
        workerClusterName: generate.name(),
      },
    };

    pipelineApi.fetchPipeline.mockImplementation(() =>
      Promise.resolve({ data }),
    );

    connectorApi.stopConnector.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    const { getByTestId } = await waitForElement(() =>
      renderWithRouter(<PipelineNewPage {...props} match={match} />),
    );

    // Stop the pipeline
    fireEvent.click(await waitForElement(() => getByTestId('stop-btn')));

    expect(connectorApi.stopConnector).toHaveBeenCalledTimes(2);
    expect(connectorApi.stopConnector).toHaveBeenCalledWith(
      data.result.objects[0].name,
    );
    expect(connectorApi.stopConnector).toHaveBeenCalledWith(
      data.result.objects[1].name,
    );
  });
});
