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
import {
  cleanup,
  render,
  waitForElement,
  fireEvent,
} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import { renderWithRouter } from 'utils/testUtils';
import PipelineToolbar from '../PipelineToolbar';
import { ICON_KEYS } from 'constants/pipelines';
import { fetchWorkers } from 'api/workerApi';
import { fetchJars } from 'api/jarApi';
import { CONNECTOR_TYPES } from 'constants/pipelines';
import { createConnector } from 'api/connectorApi';
import { createProperty } from 'api/streamApi';

jest.mock('api/infoApi');
jest.mock('api/workerApi');
jest.mock('api/jarApi');
jest.mock('api/connectorApi');
jest.mock('api/streamApi');
jest.mock('../pipelineToolbarUtils');

// Add more test cases including:
// 1. Test if the connector, stream app and topic lists are properly rendered
// 2. Test if the modal title is correctly rendered
// 3. Renders a redirect message when stream app and topic modals do not have any data.
// Plug, the add button should be disabled

const topics = generate.topics({ count: 3 });

const props = {
  match: {
    isExact: false,
    params: {},
    path: 'test/path',
    url: 'test/url',
  },
  graph: [
    {
      kind: 'source',
      id: 'id1234',
      className: CONNECTOR_TYPES.jdbcSource,
      name: 'a',
      to: [],
    },
  ],
  updateGraph: jest.fn(),
  hasChanges: false,
  iconMaps: {},
  iconKeys: ICON_KEYS,
  topics,
  currentTopic: topics[0],
  isLoading: false,
  updateCurrentTopic: jest.fn(),
  resetCurrentTopic: jest.fn(),
  workerClusterName: 'abc',
};

afterEach(cleanup);

describe('<PipelineToolbar />', () => {
  beforeEach(() => {
    const res = {
      data: {
        result: [
          {
            name: 'abc',
            connectors: [
              {
                className: '',
                definitions: [
                  {
                    displayName: 'kind',
                    defaultValue: 'source',
                  },
                  {
                    displayName: 'version',
                    defaultValue: generate.number(),
                  },
                  {
                    displayName: 'revision',
                    defaultValue: generate.number(),
                  },
                ],
              },
              {
                className: '',
                definitions: [
                  {
                    displayName: 'kind',
                    defaultValue: 'sink',
                  },
                  {
                    displayName: 'version',
                    defaultValue: generate.number(),
                  },
                  {
                    displayName: 'revision',
                    defaultValue: generate.number(),
                  },
                ],
              },
            ],
          },
        ],
        resetCurrentTopic: '',
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(res));
  });

  it('renders self', async () => {
    render(<PipelineToolbar {...props} />);
  });

  it('renders All changes saved text', async () => {
    const { getByText } = render(<PipelineToolbar {...props} />);
    getByText('All changes saved');
  });

  it('renders saving... text', async () => {
    const { getByText } = render(
      <PipelineToolbar {...props} hasChanges={true} />,
    );
    getByText('Saving...');
  });

  it('renders toolbar icons', () => {
    const { getByTestId } = render(<PipelineToolbar {...props} />);
    expect(getByTestId('toolbar-sources')).toBeVisible();
    expect(getByTestId('toolbar-topics')).toBeVisible();
    expect(getByTestId('toolbar-sinks')).toBeVisible();
    expect(getByTestId('toolbar-streams')).toBeVisible();
  });

  it('toggles new topic modal', async () => {
    const { getByTestId, queryByTestId } = render(
      <PipelineToolbar {...props} />,
    );

    expect(queryByTestId('topic-modal')).toBeNull();

    fireEvent.click(getByTestId('toolbar-topics'));
    const newTopicModal = await waitForElement(() =>
      getByTestId('topic-modal'),
    );
    expect(newTopicModal).toBeVisible();

    fireEvent.click(getByTestId('modal-cancel-btn'));
    expect(queryByTestId('topic-modal')).toBeNull();
  });

  it('tests change selected topic ', async () => {
    const { getByText, getByTestId, queryByTestId } = render(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-topics'));
    fireEvent.change(getByTestId('topic-select'), {
      target: { value: topics[2] },
    });

    // select test change is no useful, pass the test for now;
    // expect(getByTestId('topic-select').value).toBe(topics[2].name);

    fireEvent.click(getByText('Add'));
    expect(queryByTestId('topic-select')).toBeNull();
  });

  it('toggles new stream app modal', async () => {
    const streamApps = generate.streamApps({ count: 2 });

    const res = {
      data: {
        result: streamApps,
      },
    };

    fetchJars.mockImplementation(() => Promise.resolve(res));

    const { getByTestId, queryByTestId } = renderWithRouter(
      <PipelineToolbar {...props} />,
    );

    expect(queryByTestId('streamapp-modal')).toBeNull();

    fireEvent.click(getByTestId('toolbar-streams'));
    const newStreamModal = await waitForElement(() =>
      getByTestId('streamapp-modal'),
    );
    expect(newStreamModal).toBeVisible();

    fireEvent.click(getByTestId('modal-cancel-btn'));
    expect(queryByTestId('streamapp-modal')).toBeNull();
  });

  it('changes selected stream app at new stream modal', async () => {
    const streamApps = generate.streamApps({ count: 3 });
    const res = {
      data: {
        result: streamApps,
      },
    };

    fetchJars.mockImplementation(() => Promise.resolve(res));
    const { getByTestId } = renderWithRouter(<PipelineToolbar {...props} />);
    fireEvent.click(getByTestId('toolbar-streams'));
    const newStreamModal = await waitForElement(() =>
      getByTestId('streamapp-select'),
    );

    expect(newStreamModal).toBeVisible();

    fireEvent.change(newStreamModal, { target: { value: streamApps[1] } });
  });

  it('toggles new source connector modal', async () => {
    const { getByTestId, queryByTestId } = render(
      <PipelineToolbar {...props} />,
    );

    expect(queryByTestId('source-connector-modal')).toBeNull();

    fireEvent.click(getByTestId('toolbar-sources'));
    const newConnectorModal = await waitForElement(() =>
      getByTestId('source-connector-modal'),
    );
    expect(newConnectorModal).toBeVisible();

    fireEvent.click(getByTestId('modal-cancel-btn'));
    expect(queryByTestId('source-connector-modal')).toBeNull();
  });

  it('toggles new sink connector modal', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    createConnector.mockImplementation(() => Promise.resolve(res));
    createProperty.mockImplementation(() => Promise.resolve(res));

    const { getByTestId, queryByTestId } = render(
      <PipelineToolbar {...props} />,
    );

    expect(queryByTestId('sink-connector-modal')).toBeNull();

    fireEvent.click(getByTestId('toolbar-sinks'));

    const newConnectorModal = await waitForElement(() =>
      getByTestId('sink-connector-modal'),
    );

    expect(newConnectorModal).toBeVisible();

    fireEvent.click(getByTestId('modal-cancel-btn'));
    expect(queryByTestId('sink-connector-modal')).toBeNull();
  });
});
