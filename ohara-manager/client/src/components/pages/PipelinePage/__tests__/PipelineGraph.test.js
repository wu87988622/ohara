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
import { CONNECTOR_TYPES } from 'constants/pipelines';

const props = {
  iconMaps: {},
  graph: [
    {
      kind: 'source',
      className: CONNECTOR_TYPES.jdbcSource,
      name: 'a',
      to: [],
    },
    {
      kind: 'topic',
      className: CONNECTOR_TYPES.topic,
      name: 'b',
      to: [],
    },
    {
      kind: 'sink',
      className: CONNECTOR_TYPES.ftpSink,
      name: 'c',
      to: [],
    },
  ],
  pipeline: {
    workerClusterName: 'wk00',
  },
  resetGraph: jest.fn(),
  updateGraph: jest.fn(),
  match: {
    params: {
      pipelineName: 'abc',
    },
  },
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
