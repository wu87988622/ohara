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

import AppWrapper from '../AppWrapper';

const children = () => <p>children</p>;

const props = {
  title: 'test',
  children: children(),
};

describe('<AppWrapper />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<AppWrapper {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('Wrapper');
  });

  it('renders <H2 />', () => {
    expect(wrapper.find('H2').length).toBe(1);
  });

  it('renders <Main />', () => {
    expect(wrapper.find('Main').length).toBe(1);
  });

  it('renders children', () => {
    expect(wrapper.find('p').length).toBe(1);
    expect(
      wrapper
        .find('p')
        .children()
        .text(),
    ).toBe('children');
  });
});
