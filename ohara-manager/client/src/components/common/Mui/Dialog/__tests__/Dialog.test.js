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
import { cleanup, render, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Dialog from '../Dialog';

const setup = (override = {}) => {
  return {
    title: generate.name(),
    open: true,
    handleClose: jest.fn(),
    handleConfirm: jest.fn(),
    children: generate.message(),
    loading: false,
    testId: generate.name(),
    ...override,
  };
};

describe('<Dialog />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders dialog', () => {
    const props = setup();

    render(<Dialog {...props} />);
  });

  it('renders dialog title', () => {
    const props = setup();

    const { getByText } = render(<Dialog {...props} />);
    getByText(props.title);
  });

  it('renders children', () => {
    const props = setup();
    const { getByText } = render(<Dialog {...props} />);

    getByText(props.children);
  });

  it('renders dialog confirm and cancel button', () => {
    const props = setup();

    const { getByText } = render(<Dialog {...props} />);
    getByText('ADD');
    getByText('CANCEL');
  });

  it('should enable the dialog confirm button via confirmDisabled prop', () => {
    const props = setup({ confirmDisabled: false });

    const { getByText } = render(<Dialog {...props} />);
    expect(getByText('ADD')).toBeEnabled();
  });

  it('should disable the dialog confirm button via confirmDisabled prop', () => {
    const props = setup({ confirmDisabled: true });

    const { getByText } = render(<Dialog {...props} />);
    expect(getByText('ADD')).not.toBeEnabled();
  });

  it('renders the loading indicator if the loading is true', () => {
    const props = setup({ loading: true });

    const { getByTestId } = render(<Dialog {...props} />);
    getByTestId('dialog-loader');
  });

  it('renders custom confirm button text', () => {
    const confirmBtnText = generate.name();
    const props = setup({ confirmBtnText });

    const { getByText } = render(<Dialog {...props} />);
    getByText(confirmBtnText);
  });

  it('renders custom cancel button text', () => {
    const cancelBtnText = generate.name();
    const props = setup({ cancelBtnText });

    const { getByText } = render(<Dialog {...props} />);
    getByText(cancelBtnText);
  });

  it('handles confirm button click', () => {
    const props = setup();
    const { getByText } = render(<Dialog {...props} />);

    fireEvent.click(getByText('ADD'));
    expect(props.handleConfirm).toHaveBeenCalledTimes(1);
  });

  it('handles cancel button click', () => {
    const props = setup();
    const { getByText } = render(<Dialog {...props} />);

    fireEvent.click(getByText('CANCEL'));
    expect(props.handleClose).toHaveBeenCalledTimes(1);
  });
});
