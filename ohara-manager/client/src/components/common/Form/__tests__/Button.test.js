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

import Button from '../Button';

const props = {
  text: 'test',
  handleClick: jest.fn(),
};

describe('<Button />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Button {...props} />);
  });

  it('renders self', () => {
    const _props = wrapper.props();
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('Button');
    expect(_props.type).toBe('submit');
    expect(_props.width).toBe('auto');
    expect(_props.disabled).toBe(false);
  });

  it('handles click', () => {
    expect(props.handleClick).toHaveBeenCalledTimes(0);

    wrapper.simulate('click');
    expect(props.handleClick).toHaveBeenCalledTimes(1);
  });

  it('adds is-working class name and <Icon /> when props.isWorking is true', () => {
    expect(wrapper.props().className.trim()).toBe('');
    expect(wrapper.find('Icon').length).toBe(0);

    wrapper = shallow(<Button {...props} isWorking />);
    expect(wrapper.find('Icon').length).toBe(1);
    expect(wrapper.props().className).toMatch(/is-working/);
  });

  it('adds is-disabled class name and disabled prop when props.disabled is true', () => {
    expect(wrapper.props().className.trim()).toBe('');

    wrapper = shallow(<Button {...props} disabled />);
    expect(wrapper.props().className).toMatch(/is-disabled/);
    expect(wrapper.props().disabled).toBe(true);
  });
});
