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
import Container from '../Container';

const setup = (override = {}) => {
  return {
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

describe('<Container />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders container', async () => {
    render(<Container {...setup()} />);
  });

  it('renders container content', async () => {
    const bodyString = generate.message();
    const props = setup({
      children: (
        <table>
          <tbody>
            <tr>
              <td>{bodyString}</td>
            </tr>
          </tbody>
        </table>
      ),
    });
    const { getByText } = render(<Container {...props} />);
    getByText(bodyString);
  });
});
