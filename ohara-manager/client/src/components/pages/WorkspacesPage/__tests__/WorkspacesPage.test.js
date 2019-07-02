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
} from 'react-testing-library';
import 'jest-dom/extend-expect';

import * as generate from 'utils/generate';
import WorkspacesPage from '../WorkspacesPage';
import { WORKSPACES } from 'constants/documentTitles';
import { fetchWorkers } from 'api/workerApi';

jest.mock('api/workerApi');

const props = {
  history: { push: jest.fn() },
};

afterEach(cleanup);

describe('<WorkspacesPage />', () => {
  beforeEach(() => {
    const res = {
      data: {
        result: [
          {
            name: generate.name(),
            nodeNames: [generate.name()],
          },
        ],
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(res));
  });

  it('renders the page', () => {
    render(<WorkspacesPage {...props} />);
  });

  it('should have the right document title', () => {
    render(<WorkspacesPage {...props} />);
    expect(document.title).toBe(WORKSPACES);
  });

  it('renders page title', async () => {
    const { getByText } = await waitForElement(() =>
      render(<WorkspacesPage {...props} />),
    );

    getByText('Workspaces');
  });

  it('toggles new workspace modal', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      render(<WorkspacesPage {...props} />),
    );

    expect(queryByTestId('new-workspace-modal')).toBeNull();

    const newButton = getByText('New workspace');
    fireEvent.click(newButton);

    const newModal = await waitForElement(() =>
      getByTestId('new-workspace-modal'),
    );

    expect(newModal).toBeVisible();
  });
});
