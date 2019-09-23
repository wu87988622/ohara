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
import * as useApi from 'components/controller';

jest.mock('components/controller');

const props = {
  history: { push: jest.fn() },
};

afterEach(cleanup);

describe('<WorkspacesPage />', () => {
  const workspaceName = generate.serviceName();
  beforeEach(() => {
    jest.spyOn(useApi, 'usePostApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useUploadApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useDeleteApi').mockImplementation(() => {
      return { deleteApi: jest.fn() };
    });

    jest.spyOn(useApi, 'useGetApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'useWaitApi').mockImplementation(() => {
      return { waitApi: jest.fn() };
    });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          data: {
            result: [
              {
                settings: {
                  name: workspaceName,
                  nodeNames: [generate.name()],
                },
              },
            ],
          },
        },
        isLoading: false,
      };
    });

    jest.spyOn(useApi, 'usePutApi').mockImplementation(() => {
      return { putApi: jest.fn() };
    });
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

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          data: {
            result: [...workers],
          },
        },
        isLoading: false,
      };
    });

    const { getAllByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    const workspaceNames = await waitForElement(() =>
      getAllByTestId('workspace-name'),
    );

    expect(workspaceNames.length).toBe(workers.length);
  });

  it('renders an redirect message when there is no nodes available', async () => {
    const { getByText, getByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return { isLoading: false };
    });

    const warning = `You don't have any nodes available yet. But you can create one in here`;

    fireEvent.click(getByText('NEW WORKSPACE'));

    expect(getByTestId('redirect-warning').textContent).toBe(warning);
  });

  it('can open the new workspace modal with the new workspace button', async () => {
    const { getByText, getByTestId, queryByTestId } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    expect(queryByTestId('new-workspace-modal')).toBeNull();

    fireEvent.click(getByText('NEW WORKSPACE'));

    const newModal = await waitForElement(() =>
      getByTestId('new-workspace-modal'),
    );

    expect(newModal).toBeVisible();
  });

  it('disables the add button when the form is not valid', async () => {
    const { getByText } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );

    fireEvent.click(getByText('NEW WORKSPACE'));
    const addButton = await waitForElement(() => getByText('ADD'));

    expect(addButton).toBeDisabled();
  });

  // Due to some reason, final form won't set the `touched` value true when
  // FireEvent with Mui, we will need to address this in the future
  it.skip('displays an error message when the invalid name is given in the name field', async () => {
    const {
      getByText,
      queryByText,
      getByPlaceholderText,
    } = await waitForElement(() =>
      renderWithProvider(<WorkspacesPage {...props} />),
    );
    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          result: [],
        },
        isLoading: false,
      };
    });

    const errorMessage = 'You only can use lower case letters and numbers';
    const validName = 'abc';
    const invalidName = 'ABC';

    fireEvent.click(getByText('NEW WORKSPACE'));
    const nameInput = getByPlaceholderText('cluster00');

    fireEvent.change(nameInput, { target: { value: validName } });
    expect(queryByText(errorMessage)).toBeNull();

    fireEvent.change(nameInput, { target: { value: invalidName } });
    expect(queryByText(errorMessage).textContent).toBe(errorMessage);
  });
});
