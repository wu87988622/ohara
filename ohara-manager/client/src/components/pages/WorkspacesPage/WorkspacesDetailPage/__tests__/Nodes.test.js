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

import { renderWithProvider } from 'utils/testUtils';
import * as generate from 'utils/generate';
import Nodes from '../Node/Nodes';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';

jest.mock('components/controller');

afterEach(cleanup);

describe('<Nodes />', () => {
  let nodes;
  let props;

  beforeEach(() => {
    nodes = generate.nodes();
    const nodeNames = nodes.map(node => node.name);
    props = {
      workspaceName: generate.serviceName(),
    };

    jest.spyOn(useApi, 'useGetApi').mockImplementation(() => {
      return { getData: jest.fn() };
    });

    jest.spyOn(useApi, 'usePutApi').mockImplementation(() => {
      return { putApi: jest.fn() };
    });

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
      if (url === `${URL.WORKER_URL}/${props.workspaceName}`) {
        return {
          data: {
            data: {
              result: {
                settings: {
                  nodeNames,
                },
              },
            },
          },
          isLoading: false,
        };
      }
      if (url === URL.NODE_URL) {
        return {
          data: {
            data: {
              isSuccess: true,
              result: nodes,
            },
          },
        };
      }
    });
  });

  it('renders the page', async () => {
    renderWithProvider(<Nodes {...props} />);
  });

  it('should properly render the table data', async () => {
    const { getByTestId, getByText } = await waitForElement(() =>
      renderWithProvider(<Nodes {...props} />),
    );

    // Make sure we're rendering the right table heads
    getByText('Node name');
    getByText('Port');
    getByText('Last modified');

    const nodeName = getByTestId('node-name').textContent;
    const nodePort = Number(getByTestId('node-port').textContent);
    const lastModified = getByTestId('node-lastModified').textContent;

    const { name, port } = nodes[0];

    expect(nodeName).toBe(name);
    expect(nodePort).toBe(port);
    expect(lastModified).toEqual(expect.any(String));
  });

  it('renders multiple nodes', async () => {
    const nodes = generate.nodes({ count: 5 });
    const nodeNames = nodes.map(node => node.name);

    jest.spyOn(useApi, 'useFetchApi').mockImplementation(url => {
      if (url === URL.WORKER_URL + '/' + props.workspaceName) {
        return {
          data: {
            data: {
              result: {
                settings: {
                  nodeNames,
                },
              },
            },
          },
          isLoading: false,
        };
      }
      if (url === URL.NODE_URL) {
        return {
          data: {
            data: {
              isSuccess: true,
              result: nodes,
            },
          },
        };
      }
    });

    const { getAllByTestId } = await renderWithProvider(<Nodes {...props} />);

    const tableNodeNames = await waitForElement(() =>
      getAllByTestId('node-name'),
    );

    expect(tableNodeNames.length).toBe(nodes.length);
  });

  it('should close the new node modal with cancel button', async () => {
    const { getByText, getByTestId } = renderWithProvider(<Nodes {...props} />);

    fireEvent.click(getByText('NEW NODE'));
    expect(getByTestId('node-new-dialog')).toBeVisible();

    fireEvent.click(getByText('CANCEL'));
    expect(getByTestId('node-new-dialog')).not.toBeVisible();
  });
});
