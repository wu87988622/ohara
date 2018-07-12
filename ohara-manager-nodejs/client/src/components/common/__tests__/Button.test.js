import React from 'react';
import { shallow } from 'enzyme';

import Button from '../Button';

const props = {
  children: 'Hi, there',
};

describe('<Button />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Button {...props} />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.type()).toBe('button');
    expect(wrapper.text()).toBe(props.children);
  });
});
