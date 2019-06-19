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

import HomePage from '../HomePage';
import { PIPELINES } from '../../../constants/urls';

const props = { match: {} };

describe('<HomePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<HomePage {...props} />);
  });

  it('should render', () => {
    expect(wrapper.length).toBe(1);
  });

  it('should render <Redirect /> and direct to /pipeline', () => {
    const match = {};
    wrapper = shallow(<HomePage match={match} />);
    expect(wrapper.name()).toBe('Redirect');
    expect(wrapper.props().to).toBe(PIPELINES);
  });
});
