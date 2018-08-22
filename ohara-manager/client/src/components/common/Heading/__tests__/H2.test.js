import React from 'react';
import { shallow } from 'enzyme';

import H2 from '../H2';

const props = {
  children: 'test',
};

describe('<H2 />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<H2 {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('H2Wrapper');
    expect(wrapper.children().text()).toBe(props.children);
  });
});
