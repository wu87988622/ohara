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
import 'jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Nodes from '../Nodes';
import { fetchNodes } from 'api/nodeApi';
import { fetchWorker } from 'api/workerApi';

jest.mock('api/nodeApi');
jest.mock('api/workerApi');

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

    const nodesRes = {
      data: {
        isSuccess: true,
        result: nodes,
      },
    };

    const workerRes = {
      data: {
        isSuccess: true,
        result: {
          nodeNames,
          brokerClusterName: generate.serviceName(),
        },
      },
    };

    fetchWorker.mockImplementation(() => Promise.resolve(workerRes));
    fetchNodes.mockImplementation(() => Promise.resolve(nodesRes));
  });

  it('renders the page', async () => {
    render(<Nodes {...props} />);
  });

  it('should properly render the table data', async () => {
    const { getByTestId, getByText } = await waitForElement(() =>
      render(<Nodes {...props} />),
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

    const nodesRes = {
      data: {
        isSuccess: true,
        result: nodes,
      },
    };

    const workerRes = {
      data: {
        isSuccess: true,
        result: {
          nodeNames,
          brokerClusterName: generate.serviceName(),
        },
      },
    };

    fetchWorker.mockImplementation(() => Promise.resolve(workerRes));
    fetchNodes.mockImplementation(() => Promise.resolve(nodesRes));

    const res = { data: { isSuccess: true, result: nodes } };
    fetchNodes.mockImplementation(() => Promise.resolve(res));

    const { getAllByTestId } = await render(<Nodes {...props} />);

    const tableNodeNames = await waitForElement(() =>
      getAllByTestId('node-name'),
    );

    expect(tableNodeNames.length).toBe(nodes.length);
  });

  it('should close the new node modal with cancel button', async () => {
    const { getByText, getByTestId } = render(<Nodes {...props} />);

    fireEvent.click(getByText('New node'));
    expect(getByTestId('node-new-dialog')).toBeVisible();

    fireEvent.click(getByText('Cancel'));
    expect(getByTestId('node-new-dialog')).not.toBeVisible();
  });
});
