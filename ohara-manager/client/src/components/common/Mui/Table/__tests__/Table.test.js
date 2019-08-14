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
import { cleanup, render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Table from '../Table';

const setup = (override = {}) => {
  return {
    headers: ['header'],
    children: (
      <tr>
        <td />
      </tr>
    ),
    isLoading: false,
    ...override,
  };
};

describe('<Table />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders table', async () => {
    render(<Table {...setup()} />);
  });

  it('renders table header', async () => {
    const props = setup();
    const { getByText } = render(<Table {...props} />);
    getByText(props.headers[0]);
  });

  it('renders table body', async () => {
    const bodyString = generate.message();
    const props = setup({
      children: (
        <tr>
          <td>{bodyString}</td>
        </tr>
      ),
    });
    const { getByText } = render(<Table {...props} />);
    getByText(bodyString);
  });

  it('renders loader', async () => {
    const props = setup({ isLoading: true });
    const { getByTestId } = render(<Table {...props} />);

    getByTestId('table-loader');
  });
});
