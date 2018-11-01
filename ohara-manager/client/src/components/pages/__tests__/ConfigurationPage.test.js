import React from 'react';
import { shallow } from 'enzyme';

import Configuration from '../ConfigurationPage.js';

const props = { history: { goBack: jest.fn() } };

describe('<Configuration />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Configuration {...props} />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Configuration');
  });
});
