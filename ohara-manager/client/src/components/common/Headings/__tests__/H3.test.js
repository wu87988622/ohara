import React from 'react';
import { shallow } from 'enzyme';

import H3 from '../H3';

const props = {
  children: 'test',
};

describe('<H3 />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<H3 {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('H3');
    expect(wrapper.children().text()).toBe(props.children);
  });
});
