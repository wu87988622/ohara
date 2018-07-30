import React from 'react';
import { shallow } from 'enzyme';

import JobsPage from '../JobsPage';

describe('<JobsPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<JobsPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Jobs');
  });
});
