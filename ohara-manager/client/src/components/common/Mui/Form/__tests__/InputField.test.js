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
import InputField from '../InputField';

const setup = (override = {}) => {
  return {
    input: {
      name: generate.name(),
      onChange: jest.fn(),
      value: generate.name(),
      'data-testid': 'input-field',
    },
    meta: {
      touched: false,
      error: undefined,
    },
    ...override,
  };
};

describe('<InputField />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders input field', () => {
    render(<InputField {...setup()} />);
  });

  it('renders input field text value', () => {
    const props = setup();
    const { getByTestId } = render(<InputField {...props} />);
    const input = getByTestId('input-field').querySelector(
      'input[type="text"]',
    );

    expect(input).toHaveProperty('value', props.input.value);
  });

  it('handles input field text value changed', () => {
    const props = setup();
    const { getByTestId } = render(<InputField {...props} />);

    const input = getByTestId('input-field').querySelector(
      'input[type="text"]',
    );
    fireEvent.change(input, { target: { value: generate.message() } });

    expect(props.input.onChange).toHaveBeenCalledTimes(1);
  });

  it('should display an error message', () => {
    const props = setup({ meta: { touched: true, error: generate.message() } });
    const { getByText } = render(<InputField {...props} />);

    getByText(props.meta.error);
  });
});
