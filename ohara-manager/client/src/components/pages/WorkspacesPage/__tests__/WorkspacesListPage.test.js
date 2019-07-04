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
import { cleanup } from '@testing-library/react';
import 'jest-dom/extend-expect';

import WorkspacesListPage from '../WorkspacesListPage';
import { renderWithRouter } from 'utils/testUtils';

const props = {
  newWorkerSuccess: jest.fn(),
  isLoading: false,
  history: {
    push: jest.fn(),
  },
};

afterEach(cleanup);

describe('<WorkspacesListPage />', () => {
  it('renders the page', () => {
    const workers = [
      {
        name: 'abc',
        nodeNames: ['c', 'd'],
        statusTopicName: 'e',
        configTopicName: 'f',
        offsetTopicName: 'g',
      },
    ];
    renderWithRouter(<WorkspacesListPage {...props} workers={workers} />);
  });

  it('renders a loader if data is not ready', () => {
    const { getByTestId } = renderWithRouter(
      <WorkspacesListPage {...props} isLoading={true} workers={[]} />,
    );
    expect(getByTestId('table-loader')).toBeInTheDocument();
  });
});
