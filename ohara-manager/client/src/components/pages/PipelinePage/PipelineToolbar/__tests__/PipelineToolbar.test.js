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
import * as URLS from 'constants/urls';
import PipelineToolbar from '../PipelineToolbar';
import { renderWithRouter } from 'utils/testUtils';
import { ICON_KEYS, CONNECTOR_TYPES } from 'constants/pipelines';
import { fetchJars } from 'api/jarApi';
import { createConnector } from 'api/connectorApi';
import { createProperty } from 'api/streamApi';

jest.mock('api/infoApi');
jest.mock('api/workerApi');
jest.mock('api/jarApi');
jest.mock('api/connectorApi');
jest.mock('api/streamApi');

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
          defaultValue: generate.revision(6),
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
          defaultValue: generate.revision(6),
        },
      ],
    },
  ],
};

afterEach(cleanup);

describe('<PipelineToolbar />', () => {
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

  it('renders new topic modal title', async () => {
    const { getByText, getByTestId } = render(<PipelineToolbar {...props} />);

    fireEvent.click(getByTestId('toolbar-topics'));
    getByText('Add a new topic');
  });

  it('renders new topic modal topic list', async () => {
    const { getByTestId } = render(<PipelineToolbar {...props} />);

    fireEvent.click(getByTestId('toolbar-topics'));

    expect(getByTestId('topic-select').options.length).toBe(topics.length);

    expect(getByTestId('topic-select').options[0].value).toBe(topics[0].name);
    expect(getByTestId('topic-select').options[1].value).toBe(topics[1].name);
    expect(getByTestId('topic-select').options[2].value).toBe(topics[2].name);
  });

  it('should display redirect info when render new topic modal without topic data', async () => {
    const { getByText, getByTestId, queryByTestId } = renderWithRouter(
      <PipelineToolbar {...props} topics={[]} currentTopic={null} />,
    );

    fireEvent.click(getByTestId('toolbar-topics'));

    expect(queryByTestId('topic-select')).toBeNull();

    getByText(
      "You don't have any topics available in this workspace yet. But you can create one in",
    );

    const expectUrl =
      generate.serverHost() +
      URLS.WORKSPACES +
      '/' +
      props.workerClusterName +
      '/topics';
    expect(getByText('here').href).toBe(expectUrl);

    expect(getByText('Add')).toBeDisabled();
  });

  it('should change the selected topic ', async () => {
    const { getByText, getByTestId, queryByTestId } = render(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-topics'));
    fireEvent.click(getByText('Add'));

    const expectedParamsForUpdateGraph = {
      update: {
        name: topics[0].name,
        className: 'topic',
        kind: 'topic',
        to: [],
      },
      dispatcher: { name: 'TOOLBAR' },
    };

    expect(props.updateGraph).toHaveBeenCalledTimes(1);
    expect(props.updateGraph).toHaveBeenCalledWith(
      expectedParamsForUpdateGraph,
    );
    expect(queryByTestId('topic-modal')).toBeNull();
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

  it('renders new stream app modal title', async () => {
    const { getByText, getByTestId } = renderWithRouter(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-streams'));

    getByText('Add a new stream app');

    fireEvent.click(getByText('Add'));

    getByText('New StreamApp Name');
  });

  it('renders new stream app modal stream app list', async () => {
    const streamApps = generate.streamApps({ count: 3 });
    const res = {
      data: {
        result: streamApps,
      },
    };

    fetchJars.mockImplementation(() => Promise.resolve(res));
    const { getByTestId } = renderWithRouter(<PipelineToolbar {...props} />);

    fireEvent.click(getByTestId('toolbar-streams'));
    const newStreamSelect = await waitForElement(() =>
      getByTestId('streamapp-select'),
    );

    expect(newStreamSelect.options.length).toBe(streamApps.length);

    expect(newStreamSelect.options[0].value).toBe(streamApps[0].name);
    expect(newStreamSelect.options[1].value).toBe(streamApps[1].name);
    expect(newStreamSelect.options[2].value).toBe(streamApps[2].name);
  });

  it('should display redirect info when render new stream app modal without stream app data', async () => {
    const streamApps = [];
    const res = {
      data: {
        result: streamApps,
      },
    };

    fetchJars.mockImplementation(() => Promise.resolve(res));

    const { getByText, getByTestId, queryByTestId } = renderWithRouter(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-streams'));
    await waitForElement(() => getByTestId('streamapp-modal'));

    expect(queryByTestId('streamapp-select')).toBeNull();

    getByText(
      "You don't have any stream jars available in this workspace yet. But you can create one in",
    );

    const expectUrl =
      generate.serverHost() +
      URLS.WORKSPACES +
      '/' +
      props.workerClusterName +
      '/streamapps';
    expect(getByText('here').href).toBe(expectUrl);

    expect(getByText('Add')).toBeDisabled();
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

    fireEvent.change(newStreamModal, { target: { value: streamApps[1].name } });

    expect(newStreamModal.value).toBe(streamApps[1].name);
  });

  it('toggles new source connector modal', async () => {
    const { getByTestId, queryByTestId } = renderWithRouter(
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

  it('renders new source connector modal title', async () => {
    const { getByText, getByTestId } = renderWithRouter(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-sources'));
    await waitForElement(() => getByTestId('source-connector-modal'));
    getByText('Add a new source connector');

    fireEvent.click(getByTestId('connector-list'));
    fireEvent.click(getByTestId('modal-confirm-btn'));
    getByText('New Connector Name');
  });

  it('renders new source connector modal source connector list', async () => {
    const connectors = [
      {
        className: CONNECTOR_TYPES.ftpSource,
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
            defaultValue: generate.revision(6),
          },
        ],
      },
      {
        className: CONNECTOR_TYPES.jdbcSource,
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
            defaultValue: generate.revision(6),
          },
        ],
      },
    ];

    const { getByText, getByTestId, getAllByTestId } = renderWithRouter(
      <PipelineToolbar {...props} connectors={connectors} />,
    );

    fireEvent.click(getByTestId('toolbar-sources'));
    const newSourceConnectorSelect = await waitForElement(() =>
      getAllByTestId('connector-list'),
    );

    getByText('connector name');
    getByText('version');
    getByText('revision');

    expect(newSourceConnectorSelect.length).toBe(connectors.length);

    expect(newSourceConnectorSelect[0].cells.length).toBe(
      connectors[0].definitions.length,
    );

    expect(newSourceConnectorSelect[0].cells[0].textContent).toBe(
      connectors[0].className,
    );

    expect(newSourceConnectorSelect[1].cells[0].textContent).toBe(
      connectors[1].className,
    );

    expect(newSourceConnectorSelect[0].cells[1].textContent).toBe(
      connectors[0].definitions[1].defaultValue.toString(),
    );
    expect(newSourceConnectorSelect[1].cells[1].textContent).toBe(
      connectors[1].definitions[1].defaultValue.toString(),
    );

    expect(newSourceConnectorSelect[0].cells[2].textContent).toBe(
      connectors[0].definitions[2].defaultValue.toString(),
    );
    expect(newSourceConnectorSelect[1].cells[2].textContent).toBe(
      connectors[1].definitions[2].defaultValue.toString(),
    );
  });

  it('should disable add button when render new source connector modal without source connector data', async () => {
    const { getByText, getByTestId, queryByTestId } = renderWithRouter(
      <PipelineToolbar {...props} connectors={[]} />,
    );

    fireEvent.click(getByTestId('toolbar-sources'));

    expect(queryByTestId('connector-list')).toBeNull();

    expect(getByText('Add')).toBeDisabled();
  });

  it('toggles new sink connector modal', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    createConnector.mockImplementation(() => Promise.resolve(res));
    createProperty.mockImplementation(() => Promise.resolve(res));

    const { getByTestId, queryByTestId } = renderWithRouter(
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

  it('renders new sink connector modal title', async () => {
    const { getByText, getByTestId } = renderWithRouter(
      <PipelineToolbar {...props} />,
    );

    fireEvent.click(getByTestId('toolbar-sinks'));
    await waitForElement(() => getByTestId('sink-connector-modal'));
    getByText('Add a new sink connector');

    fireEvent.click(getByTestId('connector-list'));
    fireEvent.click(getByTestId('modal-confirm-btn'));
    getByText('New Connector Name');
  });

  it('renders sink connector list in new sink connector modal', async () => {
    const connectors = [
      {
        className: CONNECTOR_TYPES.ftpSink,
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
            defaultValue: generate.revision(6),
          },
        ],
      },
      {
        className: CONNECTOR_TYPES.hdfsSink,
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
            defaultValue: generate.revision(6),
          },
        ],
      },
    ];

    const { getByText, getByTestId, getAllByTestId } = renderWithRouter(
      <PipelineToolbar {...props} connectors={connectors} />,
    );

    fireEvent.click(getByTestId('toolbar-sinks'));
    const newSinkConnectorSelect = await waitForElement(() =>
      getAllByTestId('connector-list'),
    );

    getByText('connector name');
    getByText('version');
    getByText('revision');

    expect(newSinkConnectorSelect.length).toBe(connectors.length);

    expect(newSinkConnectorSelect[0].cells.length).toBe(
      connectors[0].definitions.length,
    );

    expect(newSinkConnectorSelect[0].cells[0].textContent).toBe(
      connectors[0].className,
    );

    expect(newSinkConnectorSelect[1].cells[0].textContent).toBe(
      connectors[1].className,
    );

    expect(newSinkConnectorSelect[0].cells[1].textContent).toBe(
      connectors[0].definitions[1].defaultValue.toString(),
    );

    expect(newSinkConnectorSelect[1].cells[1].textContent).toBe(
      connectors[1].definitions[1].defaultValue.toString(),
    );

    expect(newSinkConnectorSelect[0].cells[2].textContent).toBe(
      connectors[0].definitions[2].defaultValue.toString(),
    );

    expect(newSinkConnectorSelect[1].cells[2].textContent).toBe(
      connectors[1].definitions[2].defaultValue.toString(),
    );
  });

  it('should disable add button when render new sink connector modal without sink connector data', async () => {
    const { getByText, getByTestId, queryByTestId } = renderWithRouter(
      <PipelineToolbar {...props} connectors={[]} />,
    );

    fireEvent.click(getByTestId('toolbar-sinks'));

    expect(queryByTestId('connector-list')).toBeNull();

    expect(getByText('Add')).toBeDisabled();
  });
});
