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

import PipelineGraph from '../PipelineGraph';

const props = {
  iconMaps: {},
  graph: [
    {
      kind: 'source',
      id: 'id1234',
      isActive: false,
      isExist: false,
      icon: 'fa-database',
    },
    {
      kind: 'topic',
      id: 'id5678',
      isActive: false,
      isExist: false,
      icon: '',
    },
    {
      kind: 'sink',
      id: 'id9112',
      isActive: false,
      isExist: false,
      icon: '',
    },
  ],
  pipeline: {
    workerClusterName: 'wk00',
  },
  resetGraph: jest.fn(),
  updateGraph: jest.fn(),
  match: {},
};

describe('<PipelineGraph />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelineGraph {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('Box');
  });

  it('renders <H5Wrapper />', () => {
    expect(wrapper.find('H5Wrapper').length).toBe(1);
    expect(
      wrapper
        .find('H5Wrapper')
        .children()
        .text(),
    ).toBe('Pipeline graph');
  });

  it('renders <Svg />', () => {
    const svg = wrapper.find('Svg');
    expect(svg.length).toBe(1);
    expect(svg.hasClass('pipeline-graph')).toBe(true);
  });
});
