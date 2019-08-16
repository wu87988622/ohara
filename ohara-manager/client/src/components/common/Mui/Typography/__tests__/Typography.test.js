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
import Typography from '../Typography';

const setup = (override = {}) => {
  return {
    variant: 'h1',
    children: (
      <table>
        <tbody>
          <tr>
            <td />
          </tr>
        </tbody>
      </table>
    ),
    ...override,
  };
};

describe('<Typography />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders typography', async () => {
    render(<Typography {...setup()} />);
  });

  it('renders typography with assigned style class', async () => {
    const props = setup({ variant: 'h3', testId: 'mui-style' });
    const { getByTestId } = render(<Typography {...props} />);

    getByTestId(props.testId).className.includes('MuiTypography-h3');
  });

  it('renders typography with content', async () => {
    const props = setup({ children: generate.message() });
    const { getByText } = render(<Typography {...props} />);

    getByText(props.children);
  });
});
