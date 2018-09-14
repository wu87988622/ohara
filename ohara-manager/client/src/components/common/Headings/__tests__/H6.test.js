import React from 'react';
import { shallow } from 'enzyme';

import H6 from '../H6';

const props = {
  children: 'test',
};

describe('<H6 />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<H6 {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('H6');
    expect(wrapper.children().text()).toBe(props.children);
  });
});
