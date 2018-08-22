import React from 'react';
import { shallow } from 'enzyme';

import Box from '../Box';

const props = {
  children: 'test',
};

describe('<Box />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Box {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('BoxWrapper');
  });

  it('renders children', () => {
    expect(wrapper.children().text()).toBe(props.children);
  });
});
