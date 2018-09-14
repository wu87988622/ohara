import React from 'react';
import { shallow } from 'enzyme';

import H4 from '../H4';

const props = {
  children: 'test',
};

describe('<H4 />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<H4 {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
    expect(wrapper.name()).toBe('H4');
    expect(wrapper.children().text()).toBe(props.children);
  });
});
