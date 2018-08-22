import React from 'react';
import { shallow } from 'enzyme';

import H5 from '../H5';

const props = {
  children: 'test',
};

describe('<H5 />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<H5 {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('H5Wrapper');
    expect(wrapper.children().text()).toBe(props.children);
  });
});
