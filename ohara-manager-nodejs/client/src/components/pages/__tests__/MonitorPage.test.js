import React from 'react';
import { shallow } from 'enzyme';

import MonitorPage from '../MonitorPage';

describe('<MonitorPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<MonitorPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Monitor');
  });
});
