import React from 'react';
import { shallow } from 'enzyme';
import HomePage from '../HomePage';
import { PIPELINE } from '../../../constants/urls';

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
    expect(wrapper.props().to).toBe(PIPELINE);
  });
});
