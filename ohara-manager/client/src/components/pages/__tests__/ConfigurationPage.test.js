import React from 'react';
import { shallow } from 'enzyme';

import Configuration from '../ConfigurationPage.js';

describe('<Configuration />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Configuration />);
  });

  it.only('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Configuration');
  });
});
