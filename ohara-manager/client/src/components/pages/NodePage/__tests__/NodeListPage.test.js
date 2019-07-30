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
import toastr from 'toastr';
import {
  cleanup,
  render,
  waitForElement,
  fireEvent,
} from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as MESSAGES from 'constants/messages';
import * as generate from 'utils/generate';
import NodeListPage from '../NodeListPage';
import { NODES } from 'constants/documentTitles';
import { fetchNodes, createNode } from 'api/nodeApi';
import { validateNode } from 'api/validateApi';

jest.mock('api/nodeApi');
jest.mock('api/validateApi');

afterEach(cleanup);

describe.skip('<NodeListPage />', () => {
  beforeEach(() => {
    const res = {
      data: {
        result: [
          {
            name: generate.name(),
            services: [
              {
                name: generate.name(),
                clusterNames: [{ clusterName: generate.name() }],
              },
            ],
            user: generate.userName(),
            port: generate.port(),
          },
        ],
      },
    };
    fetchNodes.mockImplementation(() => Promise.resolve(res));
  });

  it('renders the page', () => {
    render(<NodeListPage />);
  });

  it('renders the right document title', () => {
    render(<NodeListPage />);
    expect(document.title).toBe(NODES);
  });

  it('renders page title', () => {
    const { getByText } = render(<NodeListPage />);
    getByText('Nodes');
  });

  it('displays multiple nodes in the table', async () => {
    const nodes = generate.nodes({ count: 3 });

    const res = {
      data: {
        result: nodes,
      },
    };

    fetchNodes.mockImplementation(() => Promise.resolve(res));

    const { getAllByTestId } = await waitForElement(() =>
      render(<NodeListPage />),
    );

    const nodeNames = await waitForElement(() => getAllByTestId('node-name'));

    expect(nodeNames.length).toBe(nodes.length);
  });

  describe('<NodeNewModal />', () => {
    it('toggles new node modal', async () => {
      const { getByText, getByTestId, queryByTestId } = await waitForElement(
        () => render(<NodeListPage />),
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

    it('disables the save button when the form is not valid', async () => {
      const { getByText } = await waitForElement(() =>
        render(<NodeListPage />),
      );

      fireEvent.click(getByText('New node'));
      const saveButton = await waitForElement(() => getByText('Save'));

      expect(saveButton).toBeDisabled();
    });

    it('enables the save button when the form is valid', async () => {
      const { getByText, getByPlaceholderText } = await waitForElement(() =>
        render(<NodeListPage />),
      );

      fireEvent.click(getByText('New node'));
      const nodeInput = getByPlaceholderText('node-00');
      const portInput = getByPlaceholderText('1021');
      const userInput = getByPlaceholderText('admin');
      const passwordInput = getByPlaceholderText('password');

      fireEvent.change(nodeInput, { target: { value: generate.name() } });
      fireEvent.change(portInput, { target: { value: generate.port() } });
      fireEvent.change(userInput, { target: { value: generate.userName() } });
      fireEvent.change(passwordInput, {
        target: { value: generate.password() },
      });

      const validateNodeResponse = {
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

      const createNodeResponse = {
        data: {
          isSuccess: true,
        },
      };

      createNode.mockImplementation(() => Promise.resolve(createNodeResponse));
      validateNode.mockImplementation(() =>
        Promise.resolve(validateNodeResponse),
      );

      const testConnectionButton = getByText('Test connection');

      fireEvent.click(testConnectionButton);

      const saveButton = await waitForElement(() => getByText('Save'));
      fireEvent.click(saveButton);

      expect(toastr.success).toHaveBeenCalledTimes(1);
      expect(toastr.success).toHaveBeenCalledWith(MESSAGES.TEST_SUCCESS);
    });
  });

  describe('<NodeEditModal>', () => {
    it('toggles edit node modal', async () => {
      const { getByTestId, queryByTestId, getByText } = await waitForElement(
        () => render(<NodeListPage />),
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
        render(<NodeListPage />),
      );

      fireEvent.click(getByTestId('edit-node-icon'));
      const saveButton = await waitForElement(() => getByText('Save'));

      expect(saveButton).toBeDisabled();
    });

    it('enables save button when there is new change made in the form', async () => {
      const {
        getByText,
        getByTestId,
        getByPlaceholderText,
      } = await waitForElement(() => render(<NodeListPage />));

      fireEvent.click(getByTestId('edit-node-icon'));
      const portInput = getByPlaceholderText('1021');

      fireEvent.change(portInput, { target: { value: generate.port() } });

      expect(getByText('Save')).not.toBeDisabled();
    });
  });
});
