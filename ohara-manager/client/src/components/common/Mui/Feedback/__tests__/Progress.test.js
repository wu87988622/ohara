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
import { cleanup, render, waitForElement } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';

import * as generate from 'utils/generate';
import Progress from '../Progress';

const setup = (override = {}) => {
  return {
    steps: [],
    open: true,
    activeStep: 0,
    testId: generate.name(),
    ...override,
  };
};

describe('<Progress />', () => {
  afterEach(() => {
    cleanup();
    jest.clearAllMocks();
  });

  it('renders progress', async () => {
    render(<Progress {...setup()} />);
  });

  it('renders progress steps', async () => {
    const props = setup({ steps: [generate.name(), generate.name()] });
    const { getByText, getAllByTestId } = await waitForElement(() =>
      render(<Progress {...props} />),
    );
    getByText(props.steps[0]);
    getByText(props.steps[1]);

    const step1 = getAllByTestId(props.testId)[0].querySelector('svg');
    const step2 = getAllByTestId(props.testId)[1].querySelector('svg');

    step1.getAttribute('class').includes('MuiStepIcon-active');
    expect(step2).toHaveAttribute('class', 'MuiSvgIcon-root MuiStepIcon-root');

    getByText('1');
    getByText('2');
  });

  it('renders progress active step 1/3', async () => {
    const props = setup({
      steps: [generate.name(), generate.name(), generate.name()],
      activeStep: 1,
    });
    const { getByText, getAllByTestId, queryByText } = await waitForElement(
      () => render(<Progress {...props} />),
    );

    const step1 = getAllByTestId(props.testId)[0].querySelector('svg');
    const step2 = getAllByTestId(props.testId)[1].querySelector('svg');
    const step3 = getAllByTestId(props.testId)[2].querySelector('svg');

    step1.getAttribute('class').includes('MuiStepIcon-completed');
    step2.getAttribute('class').includes('MuiStepIcon-active');
    expect(step3).toHaveAttribute('class', 'MuiSvgIcon-root MuiStepIcon-root');

    expect(queryByText('1')).toBeNull();
    getByText('2');
    getByText('3');
  });

  it('renders progress active step 2/3', async () => {
    const props = setup({
      steps: [generate.name(), generate.name(), generate.name()],
      activeStep: 2,
    });
    const { getByText, getAllByTestId, queryByText } = await waitForElement(
      () => render(<Progress {...props} />),
    );

    const step1 = getAllByTestId(props.testId)[0].querySelector('svg');
    const step2 = getAllByTestId(props.testId)[1].querySelector('svg');
    const step3 = getAllByTestId(props.testId)[2].querySelector('svg');

    step1.getAttribute('class').includes('MuiStepIcon-completed');
    step2.getAttribute('class').includes('MuiStepIcon-completed');
    step3.getAttribute('class').includes('MuiStepIcon-active');

    expect(queryByText('1')).toBeNull();
    expect(queryByText('2')).toBeNull();
    getByText('3');
  });
});
