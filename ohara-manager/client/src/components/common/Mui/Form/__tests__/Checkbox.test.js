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
import MuiCheckbox from '../Checkbox';

const setup = (override = {}) => {
  return {
    input: {
      name: generate.name(),
      onChange: jest.fn(),
      value: generate.name(),
      checked: true,
    },
    meta: {
      touched: true,
      error: generate.message(),
    },
    ...override,
    testId: 'checkbox',
  };
};

describe('<MuiCheckbox />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders checkbox', () => {
    const props = setup();

    render(<MuiCheckbox {...props} />);
  });

  it('renders checkbox label', () => {
    const props = setup({ label: generate.name() });
    const { getByText } = render(<MuiCheckbox {...props} />);

    getByText(props.label);
  });

  it('renders checkbox tooltip', () => {
    const props = setup({ helperText: generate.message() });

    const { getByTestId } = render(<MuiCheckbox {...props} />);
    getByTestId('tooltip');
  });

  it('renders checkbox checked if the checked is true', () => {
    const props = setup();
    const { getByTestId } = render(<MuiCheckbox {...props} />);
    const checkbox = getByTestId('checkbox').querySelector(
      'input[type="checkbox"]',
    );

    expect(checkbox).toHaveProperty('checked', true);
  });

  it('should not check the checkbox if the checked is false', () => {
    const props = setup({
      input: {
        name: generate.name(),
        onChange: jest.fn(),
        value: true,
        checked: false,
      },
    });
    const { getByTestId } = render(<MuiCheckbox {...props} />);
    const checkbox = getByTestId('checkbox').querySelector(
      'input[type="checkbox"]',
    );

    expect(checkbox).toHaveProperty('checked', false);
  });

  it('handles checkbox click', () => {
    const props = setup();
    const { getByTestId } = render(<MuiCheckbox {...props} />);

    const checkbox = getByTestId('checkbox').querySelector(
      'input[type="checkbox"]',
    );
    fireEvent.click(checkbox);

    expect(props.input.onChange).toHaveBeenCalledTimes(1);
  });
});
