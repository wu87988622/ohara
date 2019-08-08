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
import * as URLS from 'constants/urls';
import Header from '../Header';
import { fetchInfo } from 'api/infoApi';
import { renderWithProvider } from 'utils/testUtils';

jest.mock('api/infoApi');

afterEach(cleanup);

describe('<Header />', () => {
  beforeEach(() => {
    const response = {
      data: {
        result: {
          versionInfo: {
            version: generate.word(),
            revision: generate.revision(),
            date: String(generate.date.past()),
          },
        },
        isSuccess: true,
      },
    };

    fetchInfo.mockImplementation(() => Promise.resolve(response));
  });

  it('renders self', () => {
    renderWithProvider(<Header />);
  });

  it('renders the brand', () => {
    const { getByText } = renderWithProvider(<Header />);
    const brand = getByText('Ohara Stream');

    expect(brand).toHaveAttribute('href', URLS.HOME);
  });

  it('renders navigation', () => {
    const { getByText, getByTestId } = renderWithProvider(<Header />);

    getByText('Pipelines');
    getByText('Nodes');
    getByText('Workspaces');

    getByTestId('pipelines-icon');
    getByTestId('nodes-icon');
    getByTestId('workspaces-icon');

    const expectPipelinesUrl = generate.serverHost() + URLS.PIPELINES;
    const expectNodesUrl = generate.serverHost() + URLS.NODES;
    const expectWorkspacesUrl = generate.serverHost() + URLS.WORKSPACES;

    expect(getByTestId('pipelines-link').href).toBe(expectPipelinesUrl);
    expect(getByTestId('nodes-link').href).toBe(expectNodesUrl);
    expect(getByTestId('workspaces-link').href).toBe(expectWorkspacesUrl);
  });

  it('toggles info modal', async () => {
    const { getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithProvider(<Header />),
    );

    expect(queryByTestId('info-modal')).toBeNull();

    fireEvent.click(getByTestId('version-btn'));
    const newInfoModal = await waitForElement(() => getByTestId('info-modal'));
    expect(newInfoModal).toBeVisible();

    fireEvent.click(getByTestId('close-btn'));
    expect(queryByTestId('info-modal')).toBeNull();
  });
});
