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
import {
  cleanup,
  render,
  waitForElement,
  fireEvent,
} from '@testing-library/react';
import { divide, floor } from 'lodash';
import 'jest-dom/extend-expect';

import * as generate from 'utils/generate';
import StreamApp from '../StreamApp';
import { renderWithTheme } from 'utils/testUtils';
import { fetchJars, deleteJar } from 'api/jarApi';

jest.mock('api/jarApi');

afterEach(cleanup);

const props = {};

describe('<StreamApp />', () => {
  let streamApps;
  beforeEach(() => {
    streamApps = generate.streamApps();
    const res = {
      data: {
        isSuccess: true,
        result: streamApps,
      },
    };

    fetchJars.mockImplementation(() => Promise.resolve(res));
  });

  it('renders the page', async () => {
    await render(<StreamApp {...props} />);
  });

  it('should properly render the table data', async () => {
    const { getByTestId } = await render(<StreamApp {...props} />);

    const { name, size } = streamApps[0];

    const streamAppName = getByTestId('streamApp-name').textContent;
    const streamAppSize = Number(getByTestId('streamApp-size').textContent);
    const lastModified = getByTestId('streamApp-lastModified').textContent;

    expect(streamAppName).toBe(name);
    expect(streamAppSize).toBe(floor(divide(size, 1024), 2));

    // It's hard to assert the output date format since the topic last modified
    // date is generated. So we're asserting it with any given string here.
    expect(lastModified).toEqual(expect.any(String));
  });

  it('renders multiple streamApps', async () => {
    const streamApps = generate.streamApps({ count: 5 });

    const res = { data: { isSuccess: true, result: streamApps } };
    fetchJars.mockImplementation(() => Promise.resolve(res));

    const { getAllByTestId } = await render(<StreamApp {...props} />);

    const streamAppNames = await waitForElement(() =>
      getAllByTestId('streamApp-name'),
    );
    expect(streamAppNames.length).toBe(streamApps.length);
  });

  it('should close the new jar modal with cancel button', async () => {
    deleteJar.mockImplementation(() => Promise.resolve({}));

    const { getByText, getByTestId } = await renderWithTheme(
      <StreamApp {...props} />,
    );

    const streamAppName = streamApps[0].name;
    fireEvent.click(getByTestId(streamAppName));
    expect(getByText('Delete jar?')).toBeVisible(); // Ensure the modal is opened by getting it's title

    fireEvent.click(getByText('Cancel'));
    expect(getByText('Delete jar?')).not.toBeVisible();
  });
});
