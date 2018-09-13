import React from 'react';
import { shallow } from 'enzyme';

import Nav from '../Nav';
import { HOME } from 'constants/urls';

describe('<Nav />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Nav />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
  });

  it('renders <Brand />', () => {
    const brand = wrapper.find('Brand');

    expect(brand.length).toBe(1);
    expect(brand.props().to).toBe(HOME);
    expect(brand.children().text()).toBe('Ohara');
  });

  it('renders correct numbers of <LinkWrapper />', () => {
    expect(wrapper.find('LinkWrapper').length).toBe(3);
  });
});
