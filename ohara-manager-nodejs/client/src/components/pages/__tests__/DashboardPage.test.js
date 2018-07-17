import React from 'react';
import { shallow } from 'enzyme';

import DashboardPage from '../DashboardPage';

describe('<DashboardPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<DashboardPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Dashboard');
  });
});
