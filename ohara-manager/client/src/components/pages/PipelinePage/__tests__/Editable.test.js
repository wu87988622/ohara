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
import toastr from 'toastr';
import { shallow } from 'enzyme';

import Editable from '../Editable';
import { getTestById } from 'utils/testUtils';
import { EMPTY_PIPELINE_TITLE_ERROR } from 'constants/messages';

const props = {
  title: 'test',
  handleChange: jest.fn(),
  handleFocusOut: jest.fn(),
};

describe('<Editable />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Editable {...props} />);
    jest.clearAllMocks();
  });

  it('renders <label />', () => {
    const label = wrapper.find('label');

    expect(label.length).toBe(1);
    expect(label.type()).toBe('label');
    expect(label.children().text()).toBe(props.title);
  });

  it('renders <Input /> when <label /> is clicked', () => {
    wrapper.find(getTestById('title-label')).simulate('click');

    const input = wrapper.find(getTestById('title-input'));
    const _props = input.props();

    expect(input.name()).toBe('Input');
    expect(_props.value).toBe(props.title);
    expect(_props.onChange).toBeDefined();
    expect(_props.onKeyDown).toBeDefined();
    expect(_props.onBlur).toBeDefined();
    expect(_props.autoFocus).toBe(true);
  });

  it('updates the <label /> content', () => {
    wrapper.find(getTestById('title-label')).simulate('click');
    const evt = { target: { value: 'a' } };
    wrapper.find(getTestById('title-input')).prop('onChange')(evt);

    expect(props.handleChange).toHaveBeenCalledTimes(1);
    expect(props.handleChange).toHaveBeenCalledWith(evt);
  });

  it('finishes editing when press the enter key', () => {
    const label = wrapper.find(getTestById('title-label'));
    const evt = { keyCode: 13 };

    label.simulate('click');

    const input = wrapper.find(getTestById('title-input'));
    expect(input.name()).toBe('Input');

    input.prop('onKeyDown')(evt);
    expect(label.length).toBe(1);
    expect(wrapper.find(getTestById('title-input')).length).toBe(0);
  });

  it('finishes editing when press the esc key', () => {
    const label = wrapper.find(getTestById('title-label'));
    const evt = { keyCode: 27 };

    label.simulate('click');
    const input = wrapper.find(getTestById('title-input'));

    expect(input.name()).toBe('Input');

    input.prop('onKeyDown')(evt);
    expect(label.name()).toBe('label');
  });

  it('displays an error message to user when leaves edit mode with empty content', () => {
    wrapper.setState({ isEditing: true });
    wrapper.setProps({ title: '' });

    const evt = { keyCode: 13 };
    wrapper.find(getTestById('title-input')).prop('onKeyDown')(evt);

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(EMPTY_PIPELINE_TITLE_ERROR);
  });

  it('leaves edit mode without empty content', () => {
    wrapper.setState({ isEditing: true });
    wrapper.setProps({ title: 'abcdefg' });

    const evt = { keyCode: 13 };
    wrapper.find(getTestById('title-input')).prop('onKeyDown')(evt);

    expect(props.handleFocusOut).toHaveBeenCalledTimes(1);
    expect(props.handleFocusOut).toHaveBeenCalledWith(true);
  });
});
