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

import Select from '../Select';

const props = {
  list: [{ name: 'a', id: '1' }, { name: 'b', id: '2' }],
  selected: { name: 'a', id: '2' },
  handleChange: jest.fn(),
  isObject: true,
};

describe('<Select />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Select {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
  });

  describe('when props.isObject is true', () => {
    it('renders the correct selected item', () => {
      expect(wrapper.props().value).toBe(props.selected.name);
    });

    it('renders list items correctly', () => {
      expect(wrapper.find('option').length).toBe(2);

      expect(
        wrapper.find('option').forEach((option, idx) => {
          expect(option.text()).toBe(props.list[idx].name);
        }),
      );
    });
  });

  describe('when props.isObject is false', () => {
    beforeEach(() => {
      wrapper = shallow(<Select {...props} isObject={false} />);
    });

    it('renders the correct selected item', () => {
      expect(wrapper.props().value).toBe(props.selected);
    });

    it('renders list items correctly', () => {
      expect(wrapper.find('option').length).toBe(2);
    });

    // TODO: fix the weird prop type issue
  });

  it('handles onChange', () => {
    const evt = { e: { target: { value: 'b' } } };

    expect(props.handleChange).toHaveBeenCalledTimes(0);

    wrapper.simulate('change', evt);
    expect(props.handleChange).toHaveBeenCalledTimes(1);
    expect(props.handleChange).toBeCalledWith(evt);
  });
});
