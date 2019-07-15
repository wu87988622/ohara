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
import WorkspacesPage from '../WorkspacesPage';
import { WORKSPACES } from 'constants/documentTitles';
import { fetchWorkers } from 'api/workerApi';

jest.mock('api/workerApi');

const props = {
  history: { push: jest.fn() },
};

afterEach(cleanup);

describe('<WorkspacesPage />', () => {
  const workspaceName = generate.serviceName();
  beforeEach(() => {
    const res = {
      data: {
        result: [
          {
            name: workspaceName,
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

  it('displays a workspace in the table', async () => {
    const { getByText } = await waitForElement(() =>
      render(<WorkspacesPage {...props} />),
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
      render(<WorkspacesPage {...props} />),
    );

    const workspaceNames = await waitForElement(() =>
      getAllByTestId('workspace-name'),
    );

    expect(workspaceNames.length).toBe(workers.length);
  });

  it('can open the new workspace modal with the new workspace button', async () => {
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

  it('disables the add button when the form is not valid', async () => {
    const { getByText } = await waitForElement(() =>
      render(<WorkspacesPage {...props} />),
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
    } = await waitForElement(() => render(<WorkspacesPage {...props} />));

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
