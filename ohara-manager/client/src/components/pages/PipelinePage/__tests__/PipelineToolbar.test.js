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

import PipelineToolbar from '../PipelineToolbar';
import { ICON_KEYS } from 'constants/pipelines';
import { getTestById } from 'utils/testUtils';
import { fetchInfo } from 'api/infoApi';
import { CONNECTOR_TYPES } from 'constants/pipelines';

jest.mock('api/infoApi');

const props = {
  match: {
    isExact: false,
    params: {},
    path: 'test/path',
    url: 'test/url',
  },
  graph: [
    {
      kind: 'source',
      id: 'id1234',
      className: CONNECTOR_TYPES.jdbcSource,
      name: 'a',
      to: [],
    },
  ],
  updateGraph: jest.fn(),
  hasChanges: false,
  iconMaps: {},
  iconKeys: ICON_KEYS,
  topics: [],
  currentTopic: {},
  isLoading: false,
  updateCurrentTopic: jest.fn(),
  resetCurrentTopic: jest.fn(),
  workerClusterName: 'abc',
};

describe('<PipelineToolbar />', () => {
  let wrapper;
  beforeEach(() => {
    fetchInfo.mockImplementation(() =>
      Promise.resolve({ sources: [], sinks: [] }),
    );

    wrapper = shallow(<PipelineToolbar {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('ToolbarWrapper');
  });

  it('renders <FileSavingStatus />', () => {
    expect(wrapper.find('FileSavingStatus').length).toBe(1);
  });

  it('renders the correct status base on this.props.hasChanges', () => {
    expect(
      wrapper
        .find('FileSavingStatus')
        .children()
        .text(),
    ).toBe('All changes saved');

    wrapper = shallow(<PipelineToolbar {...props} hasChanges={true} />);
    wrapper.update();
    expect(
      wrapper
        .find('FileSavingStatus')
        .children()
        .text(),
    ).toBe('Saving...');
  });

  it('renders sources icon', () => {
    expect(wrapper.find(getTestById('toolbar-sources')).length).toBe(1);
  });

  it('renders topic icon', () => {
    expect(wrapper.find(getTestById('toolbar-topics')).length).toBe(1);
  });

  it('renders sink icon', () => {
    expect(wrapper.find(getTestById('toolbar-sinks')).length).toBe(1);
  });

  it('renders streams icon', () => {
    expect(wrapper.find(getTestById('toolbar-streams')).length).toBe(1);
  });
});
