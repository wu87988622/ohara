import React from 'react';
import { shallow } from 'enzyme';

import HomePage from '../HomePage';

const props = { match: {} };

describe('<HomePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<HomePage {...props} />);
  });

  it('should render', () => {
    expect(wrapper.length).toBe(1);
  });

  it('should render the correct h2', () => {
    expect(wrapper.find('H2').length).toBe(1);
    expect(
      wrapper
        .find('H2')
        .children()
        .text(),
    ).toBe('Pipeline');
  });
});
