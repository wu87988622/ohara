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

import HdfsSink from '../HdfsSink';
import { getTestById } from 'utils/testUtils';
import { CONFIGURATION } from 'constants/urls';

const props = {
  hasChanges: false,
  updateHasChanges: jest.fn(),
  updateGraph: jest.fn(),
  loadGraph: jest.fn(),
  graph: [],
  topics: [{ name: 'a', id: '1234' }, { name: 'b', id: '5678' }],
  match: {
    params: {
      connectorId: '12345',
    },
  },
  schema: [],
  isRedirect: false,
  refreshGraph: jest.fn(),
  history: {
    push: jest.fn(),
  },
  isPipelineRunning: false,
};

describe('<HdfsSink />', () => {
  let wrapper;

  beforeEach(() => {
    wrapper = shallow(<HdfsSink {...props} />);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <H5 /> ', () => {
    const h5 = wrapper.find('H5');
    expect(h5.length).toBe(1);
    expect(h5.children().text()).toBe('HDFS sink connector');
  });

  it('renders read from topic <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('read-from-topic'));
    const label = fromGroup.find('Label');
    const select = fromGroup.find('Select');
    const _props = select.props();

    expect(label.children().text()).toBe('Read topic');
    expect(select.length).toBe(1);
    expect(select.name()).toBe('Select');
    expect(_props).toHaveProperty('isObject');
    expect(_props.name).toBe('topics');
    expect(_props.selected).toBe(wrapper.state().currReadTopic);
    expect(_props.width).toBe('100%');
    expect(_props.handleChange).toBeDefined();
  });

  it('renders hdfs connection url <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('hdfsConnectionUrl'));
    const label = fromGroup.find('Label');
    const input = fromGroup.find('Input');
    const _props = input.props();

    expect(label.children().text()).toBe('Connection URL');
    expect(input.length).toBe(1);
    expect(input.name()).toBe('Input');
    expect(_props.name).toBe('hdfsConnectionUrl');
    expect(_props.width).toBe('100%');
    expect(_props.placeholder).toBe('file://');
    expect(_props.value).toEqual(wrapper.state().writePath);
    expect(_props.handleChange).toBeDefined();
  });

  it('renders write path <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('write-path'));
    const label = fromGroup.find('Label');
    const input = fromGroup.find('Input');
    const _props = input.props();

    expect(label.children().text()).toBe('Write path');
    expect(input.length).toBe(1);
    expect(input.name()).toBe('Input');
    expect(_props.name).toBe('writePath');
    expect(_props.width).toBe('100%');
    expect(_props.placeholder).toBe('file://');
    expect(_props.value).toEqual(wrapper.state().writePath);
    expect(_props.handleChange).toBeDefined();
  });

  it('renders temp directory <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('temp-directory'));
    const label = fromGroup.find('Label');
    const input = fromGroup.find('Input');
    const _props = input.props();

    expect(label.children().text()).toBe('Temp directory');
    expect(input.length).toBe(1);
    expect(input.name()).toBe('Input');
    expect(_props.name).toBe('tempDirectory');
    expect(_props.width).toBe('100%');
    expect(_props.placeholder).toBe('/tmp');
    expect(_props.value).toEqual(wrapper.state().tempDirectory);
    expect(_props.handleChange).toBeDefined();
  });

  it('renders include header <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('need-header'));
    const checkbox = fromGroup.find('Checkbox');
    const _props = checkbox.props();

    expect(checkbox.length).toBe(1);
    expect(checkbox.name()).toBe('Checkbox');
    expect(_props.name).toBe('needHeader');
    expect(_props.width).toBe('25px');
    expect(_props.value).toEqual('');
    expect(_props.handleChange).toBeDefined();
  });

  it('renders file encoding <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('file-encoding'));
    const label = fromGroup.find('Label');
    const select = fromGroup.find('Select');
    const _props = select.props();

    expect(label.children().text()).toBe('File encoding');
    expect(select.length).toBe(1);
    expect(select.name()).toBe('Select');
    expect(_props.name).toBe('fileEncoding');
    expect(_props.width).toBe('100%');
    expect(_props.list).toEqual(wrapper.state().fileEncodings);
    expect(_props.selected).toBe(wrapper.state().currFileEncoding);
    expect(_props.handleChange).toBeDefined();
  });

  it('renders rotate interval <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('rotate-interval'));
    const label = fromGroup.find('Label');
    const input = fromGroup.find('Input');
    const _props = input.props();

    expect(label.children().text()).toBe('Rotate interval (ms)');
    expect(input.length).toBe(1);
    expect(input.name()).toBe('Input');
    expect(_props.name).toBe('rotateInterval');
    expect(_props.width).toBe('100%');
    expect(_props.placeholder).toBe('60000');
    expect(_props.value).toEqual(wrapper.state().rotateInterval);
    expect(_props.handleChange).toBeDefined();
  });

  it('renders flush line count <FromGroup>', () => {
    const fromGroup = wrapper.find(getTestById('flush-line-count'));
    const label = fromGroup.find('Label');
    const input = fromGroup.find('Input');
    const _props = input.props();

    expect(label.children().text()).toBe('Flush line count');
    expect(input.length).toBe(1);
    expect(input.name()).toBe('Input');
    expect(_props.name).toBe('flushLineCount');
    expect(_props.width).toBe('100%');
    expect(_props.placeholder).toBe('10');
    expect(_props.value).toEqual(wrapper.state().flushLineCount);
    expect(_props.handleChange).toBeDefined();
  });

  it('should render <Redirect /> when this.state.isRedirect is true', () => {
    wrapper.setState({ isRedirect: true });

    expect(wrapper.name()).toBe('Redirect');
    expect(wrapper.props().to).toBe(CONFIGURATION);
  });
});
