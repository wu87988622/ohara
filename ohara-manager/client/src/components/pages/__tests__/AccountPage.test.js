import React from 'react';
import { shallow } from 'enzyme';

import AccountPage from '../AccountPage';

describe('<AccountPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<AccountPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Account');
  });
});
