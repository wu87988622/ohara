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
import { cleanup, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import DeleteDialog from '../DeleteDialog';
import { renderWithProvider } from 'utils/testUtils';

const setup = (override = {}) => {
  return {
    title: generate.name(),
    content: generate.message(),
    open: true,
    handleClose: jest.fn(),
    handleConfirm: jest.fn(),
    ...override,
  };
};

describe('<DeleteDialog />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders delete dialog', () => {
    const props = setup();
    renderWithProvider(<DeleteDialog {...props} />);
  });

  it('renders delete dialog title', () => {
    const props = setup();

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    getByText(props.title);
  });

  it('renders content', () => {
    const props = setup();
    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);

    getByText(props.content);
  });

  it('renders dialog confirm and cancel button', () => {
    const props = setup();

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    getByText('Delete');
    getByText('Cancel');
  });

  it('should enable the dialog confirm button via isWorking prop', () => {
    const props = setup({ isWorking: false });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    expect(getByText('Delete')).toBeEnabled();
  });

  it('should disable the dialog confirm button via isWorking prop', () => {
    const props = setup({ isWorking: true });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    expect(getByText('Delete')).not.toBeEnabled();
  });

  it('should enable the dialog cancel button via isWorking prop', () => {
    const props = setup({ isWorking: false });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    expect(getByText('Cancel')).toBeEnabled();
  });

  it('should disable the dialog cancel button via isWorking prop', () => {
    const props = setup({ isWorking: true });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    expect(getByText('Cancel')).not.toBeEnabled();
  });

  it('renders the loading indicator if the isWorking is true', () => {
    const props = setup({ isWorking: true });

    const { getByTestId } = renderWithProvider(<DeleteDialog {...props} />);
    getByTestId('dialog-loader');
  });

  it('renders custom confirm button text', () => {
    const confirmText = generate.name();
    const props = setup({ confirmText });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    getByText(confirmText);
  });

  it('renders custom cancel button text', () => {
    const cancelText = generate.name();
    const props = setup({ cancelText });

    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);
    getByText(cancelText);
  });

  it('handles confirm button click', () => {
    const props = setup();
    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);

    fireEvent.click(getByText('Delete'));
    expect(props.handleConfirm).toHaveBeenCalledTimes(1);
  });

  it('handles cancel button click', () => {
    const props = setup();
    const { getByText } = renderWithProvider(<DeleteDialog {...props} />);

    fireEvent.click(getByText('Cancel'));
    expect(props.handleClose).toHaveBeenCalledTimes(1);
  });
});
