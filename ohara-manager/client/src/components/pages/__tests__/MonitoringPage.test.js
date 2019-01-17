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

import MonitoringPage from '../MonitoringPage';
import { MONITORING } from 'constants/documentTitles';

describe('<MonitoringPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<MonitoringPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(MONITORING);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Monitoring');
  });
});
