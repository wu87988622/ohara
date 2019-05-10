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
import { cleanup, render, fireEvent } from 'react-testing-library';
import 'jest-dom/extend-expect';

import WorkerNewModal from '../WorkerNewModal';
import { createWorker } from 'api/workerApi';
import { fetchBrokers } from 'api/brokerApi';

jest.mock('api/workerApi');
jest.mock('api/brokerApi');

const props = {
  isActive: true,
  onClose: jest.fn(),
  onConfirm: jest.fn(),
};

afterEach(cleanup);

describe('<WorkerNewModal />', () => {
  it('renders the page', () => {
    render(<WorkerNewModal {...props} />);
  });

  it('submits the form', async () => {
    const workerRes = {
      data: {
        result: [{ name: 'wk00' }],
        isSuccess: true,
      },
    };
    const brokerRes = {
      data: {
        result: [{ name: 'bk00' }],
        isSuccess: true,
      },
    };
    fetchBrokers.mockImplementation(() => Promise.resolve(brokerRes));
    createWorker.mockImplementation(() => Promise.resolve(workerRes));

    const { getByPlaceholderText, getByText } = render(
      <WorkerNewModal {...props} />,
    );

    const serviceInput = getByPlaceholderText('cluster00');
    const portInput = getByPlaceholderText('5000');
    const addBtn = getByText('Add');

    const name = 'mycluster';
    const port = '5555';

    fireEvent.change(serviceInput, { target: { value: name } });
    fireEvent.change(portInput, { target: { value: port } });
    await fireEvent.click(addBtn);

    const expected = {
      name,
      clientPort: port,
      plugins: [],
      jmxPort: expect.any(Number),
      brokerClusterName: 'bk00',
    };

    expect(createWorker).toHaveBeenCalledTimes(1);
    expect(createWorker).toHaveBeenCalledWith(expected);
  });
});
