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
import { shallow } from 'enzyme';

import WorkerListPage from '../WorkerListPage';

const props = {
  workers: [
    {
      name: 'abc',
      nodeNames: ['c', 'd'],
      statusTopicName: 'e',
      configTopicName: 'f',
      offsetTopicName: 'g',
    },
  ],
  newWorkerSuccess: jest.fn(),
  isLoading: false,
};

describe('<WorkerListPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<WorkerListPage {...props} />);
  });

  it('renders the page', () => {
    expect(wrapper.length).toBe(1);
  });

  it('disables new cluster button when there are more than one workers in the list', () => {
    expect(wrapper.find('NewClusterBtn').props().disabled).toBe(true);
  });

  it('should enable the new cluster button so users can create workers with it', async () => {
    const workers = [];
    wrapper = shallow(<WorkerListPage {...props} workers={workers} />);

    expect(wrapper.find('NewClusterBtn').props().disabled).toBe(false);
  });
});
