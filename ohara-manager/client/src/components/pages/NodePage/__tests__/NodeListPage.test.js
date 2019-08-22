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
import NodeListPage from '../NodeListPage';
import { NODES } from 'constants/documentTitles';
import { renderWithProvider } from 'utils/testUtils';
import * as useApi from 'components/controller';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

jest.mock('components/controller');
jest.mock('api/validateApi');
jest.mock('components/context/Snackbar/useSnackbar');

afterEach(cleanup);

describe('<NodeListPage />', () => {
  beforeEach(() => {
    useSnackbar.mockImplementation(() => {
      return {
        showMessage: jest.fn(),
      };
    });
    jest.spyOn(useApi, 'usePostApi').mockImplementation(() => {
      return {
        getData: jest.fn(),
        postApi: jest.fn(),
      };
    });

    jest.spyOn(useApi, 'useValidationApi').mockImplementation(() => {
      return {
        getData: () => {
          return {
            data: {
              result: [
                {
                  pass: true,
                },
              ],
            },
          };
        },
        validationApi: jest.fn(),
      };
    });

    jest.spyOn(useApi, 'usePutApi').mockImplementation(() => {
      return {
        getData: jest.fn(),
      };
    });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          data: {
            result: [
              {
                name: generate.serviceName(),
                services: [
                  {
                    name: generate.serviceName(),
                    clusterNames: [{ clusterName: generate.serviceName() }],
                  },
                ],
                user: generate.userName(),
                port: generate.port(),
                password: generate.serviceName(),
              },
            ],
          },
        },
        isLoading: false,
        refetch: jest.fn(),
      };
    });
  });

  it('renders the page', () => {
    renderWithProvider(<NodeListPage />);
  });

  it('renders the right document title', () => {
    renderWithProvider(<NodeListPage />);
    expect(document.title).toBe(NODES);
  });

  it('renders page title', () => {
    const { getByText } = renderWithProvider(<NodeListPage />);
    getByText('Nodes');
  });

  it('displays multiple nodes in the table', async () => {
    const nodes = generate.nodes({ count: 3 });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(() => {
      return {
        data: {
          data: {
            result: nodes,
          },
        },
        isLoading: false,
      };
    });

    const { getAllByTestId } = await waitForElement(() =>
      renderWithProvider(<NodeListPage />),
    );

    const nodeNames = await waitForElement(() => getAllByTestId('node-name'));

    expect(nodeNames.length).toBe(nodes.length);
  });

  describe('<NodeNewModal />', () => {
    it('toggles new node modal', async () => {
      const { getByText, getByTestId, queryByTestId } = await waitForElement(
        () => renderWithProvider(<NodeListPage />),
      );

      expect(queryByTestId('new-node-modal')).toBeNull();

      const newButton = getByText('New node');
      fireEvent.click(newButton);
      const newModal = await waitForElement(() =>
        getByTestId('new-node-modal'),
      );

      expect(newModal).toBeVisible();

      const cancelButton = getByText('Cancel');
      fireEvent.click(cancelButton);

      expect(queryByTestId('new-node-modal')).not.toBeVisible();
    });

    it('disables the add button when the form is not valid', async () => {
      const { getByText } = await waitForElement(() =>
        renderWithProvider(<NodeListPage />),
      );

      fireEvent.click(getByText('New node'));
      const addButton = await waitForElement(() => getByText('Add'));

      expect(addButton).toBeDisabled();
    });

    it('enables the add button when the form is valid', async () => {
      const { getByText, getByPlaceholderText } = await waitForElement(() =>
        renderWithProvider(<NodeListPage />),
      );

      fireEvent.click(getByText('New node'));
      const nodeInput = getByPlaceholderText('node-01');
      const portInput = getByPlaceholderText('22');
      const userInput = getByPlaceholderText('admin');
      const passwordInput = getByPlaceholderText('password');

      fireEvent.change(nodeInput, { target: { value: generate.name() } });
      fireEvent.change(portInput, { target: { value: generate.port() } });
      fireEvent.change(userInput, { target: { value: generate.userName() } });
      fireEvent.change(passwordInput, {
        target: { value: generate.password() },
      });

      jest.spyOn(useApi, 'usePostApi').mockImplementation(() => {
        return {
          getData: () => {
            return {
              data: {
                isSuccess: true,
              },
            };
          },
          postApi: jest.fn(),
        };
      });

      jest.spyOn(useApi, 'useValidationApi').mockImplementation(() => {
        return {
          getData: () => {
            return {
              data: {
                result: [
                  {
                    hostname: generate.name(),
                    message: generate.message(),
                    pass: true,
                  },
                ],
              },
            };
          },
        };
      });
      jest.clearAllMocks();
      const testConnectionButton = getByText('Test connection');

      fireEvent.click(testConnectionButton);

      const addButton = await waitForElement(() => getByText('Add'));
      fireEvent.click(addButton);

      expect(useSnackbar).toHaveBeenCalledTimes(1);
    });
  });

  describe('<NodeEditModal>', () => {
    it('toggles edit node modal', async () => {
      const { getByTestId, queryByTestId, getByText } = await waitForElement(
        () => renderWithProvider(<NodeListPage />),
      );

      expect(queryByTestId('edit-node-modal')).toBeNull();

      const editButton = getByTestId('edit-node-icon');
      fireEvent.click(editButton);

      const editModal = await waitForElement(() =>
        getByTestId('edit-node-modal'),
      );

      expect(editModal).toBeVisible();

      const cancelButton = getByText('Cancel');
      fireEvent.click(cancelButton);

      expect(queryByTestId('edit-node-modal')).not.toBeVisible();
    });

    it('disables the save button when the form is not valid', async () => {
      const { getByText, getByTestId } = await waitForElement(() =>
        renderWithProvider(<NodeListPage />),
      );

      fireEvent.click(getByTestId('edit-node-icon'));
      const saveButton = await waitForElement(() => getByText('Save'));

      expect(saveButton).toBeDisabled();
    });

    //In jest we cant't change hooks useState, so we cant't test this task.
    it.skip('enables save button when there is new change made in the form', async () => {
      const { getByTestId, getByText } = await waitForElement(() =>
        renderWithProvider(<NodeListPage />),
      );

      fireEvent.click(getByTestId('edit-node-icon'));
      const testConnectionButton = getByText('Test connection');
      fireEvent.click(testConnectionButton);
      // expect(getByText('Save')).not.toBeDisabled();
    });
  });
});
