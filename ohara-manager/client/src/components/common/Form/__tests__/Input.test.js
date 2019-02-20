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

import Input from '../Input';

const props = {
  value: 'test input',
  handleChange: jest.fn(),
  placeholder: 'test',
};

describe('<Input />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Input {...props} />);
  });

  it('renders self', () => {
    const input = wrapper.find('Input');
    const { type, value, placeholder, disabled } = wrapper.props();

    expect(input.length).toBe(1);
    expect(type).toBe('text');
    expect(value).toBe(props.value);
    expect(placeholder).toBe(props.placeholder);
    expect(disabled).toBe(false);
  });

  it('renders disable class when disabled is set to true', () => {
    wrapper.setProps({ disabled: true });
    expect(wrapper.props().className).toMatch(/is-disabled/);
  });

  it('handles change', () => {
    const evt = { e: { target: { value: 'b' } } };

    expect(props.handleChange).toHaveBeenCalledTimes(0);
    wrapper.simulate('change', evt);
    expect(props.handleChange).toHaveBeenCalledTimes(1);
    expect(props.handleChange).toBeCalledWith(evt);
  });
});
