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
import { cleanup, fireEvent, render } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Select from '../Select';

const setup = (override = {}) => {
  return {
    input: {
      name: generate.name(),
      onChange: jest.fn(),
      value: '',
    },
    meta: {
      touched: false,
      error: null,
    },
    list: [],
    label: generate.name(),
    ...override,
  };
};

describe('<Select />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders select', async () => {
    render(<Select {...setup()} />);
  });

  it('renders select label', async () => {
    const props = setup();
    const { getByText } = render(<Select {...props} />);

    getByText(props.label);
  });

  it('displays all options in the select', async () => {
    const props = setup({ list: ['a', 'b'] });
    const { getByText } = render(<Select {...props} />);
    getByText('Please select...');

    fireEvent.click(getByText('Please select...'));

    getByText('a');
    getByText('b');
  });

  it('handles change', async () => {
    const props = setup({ list: ['apple'] });
    const { getByText } = render(<Select {...props} />);

    fireEvent.click(getByText('Please select...'));
    fireEvent.click(getByText('apple'));

    expect(props.input.onChange).toHaveBeenCalledTimes(1);
  });
});
