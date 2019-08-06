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
import Button from '../Button';

const setup = (override = {}) => {
  return {
    text: generate.name(),
    onClick: jest.fn(),
    ...override,
  };
};

describe('<Button />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders button', () => {
    const props = setup();

    render(<Button {...props} />);
  });

  it('renders button text', () => {
    const props = setup();

    const { getByText } = render(<Button {...props} />);
    getByText(props.text);
  });

  it('handles button click', () => {
    const props = setup();
    const { getByText } = render(<Button {...props} />);

    fireEvent.click(getByText(props.text));
    expect(props.onClick).toHaveBeenCalledTimes(1);
  });
});
