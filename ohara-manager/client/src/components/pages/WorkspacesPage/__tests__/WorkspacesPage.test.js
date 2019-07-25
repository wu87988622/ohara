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
import WorkspacesPage from '../WorkspacesPage';
import { renderWithProvider } from 'utils/testUtils';
import { WORKSPACES } from 'constants/documentTitles';
import { fetchWorkers } from 'api/workerApi';
import { fetchNodes } from 'api/nodeApi';

jest.mock('api/workerApi');
jest.mock('api/nodeApi');

const props = {
  history: { push: jest.fn() },
};

afterEach(cleanup);

describe.skip('<WorkspacesPage />', () => {
  const workspaceName = generate.serviceName();
  beforeEach(() => {
    const workerRes = {
      data: {
        result: [
          {
            name: workspaceName,
            nodeNames: [generate.name()],
          },
        ],
      },
    };

    const nodeRes = {
      data: {
        result: generate.nodes({ count: 2 }),
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(workerRes));
    fetchNodes.mockImplementation(() => Promise.resolve(nodeRes));
  });

  it('renders the page', () => {
    renderWithProvider(<WorkspacesPage {...props} />);
  });

  it('should have the right document title', () => {
    renderWithProvider(<WorkspacesPage {...props} />);
    expect(document.title).toBe(WORKSPACES);
  });

  it('renders page title', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    getByText('Workspaces');
  });

  it('displays a workspace in the table', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    getByText(workspaceName);
  });

  it('displays multiple workspaces in the table', async () => {
    const workers = generate.workers({ count: 3 });

    const res = {
      data: {
        result: [...workers],
      },
    };

    fetchWorkers.mockImplementation(() => Promise.resolve(res));

    const { getAllByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    const workspaceNames = await waitForElement(() =>
      getAllByTestId('workspace-name'),
    );

    expect(workspaceNames.length).toBe(workers.length);
  });

  it('renders an redirect message when there is no nodes available', async () => {
    const nodeRes = {
      data: {
        result: [],
      },
    };

    fetchNodes.mockImplementation(() => Promise.resolve(nodeRes));

    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    const warning = `You don't have any nodes available yet. But you can create one in here`;

    fireEvent.click(getByText('New workspace'));

    expect(getByTestId('redirect-warning').textContent).toBe(warning);
  });

  it('can open the new workspace modal with the new workspace button', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    expect(queryByTestId('new-workspace-modal')).toBeNull();

    fireEvent.click(getByText('New workspace'));

    const newModal = await waitForElement(() =>
      getByTestId('new-workspace-modal'),
    );

    expect(newModal).toBeVisible();
  });

  it('disables the add button when the form is not valid', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    fireEvent.click(getByText('New workspace'));
    const addButton = await waitForElement(() => getByText('Add'));

    expect(addButton).toBeDisabled();
  });

  it('displays an error message when the invalid name is given in the name field', async () => {
    const {
      getByText,
      queryByText,
      getByPlaceholderText,
    } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    const errorMessage = 'You only can use lower case letters and numbers';
    const validName = 'abc';
    const invalidName = 'ABC';

    fireEvent.click(getByText('New workspace'));
    const nameInput = getByPlaceholderText('cluster00');

    fireEvent.change(nameInput, { target: { value: validName } });
    expect(queryByText(errorMessage)).toBeNull();

    fireEvent.change(nameInput, { target: { value: invalidName } });
    expect(queryByText(errorMessage).textContent).toBe(errorMessage);
  });
});
