import React from 'react';
import { shallow } from 'enzyme';

import Label from '../Label';

describe('<Label />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Label>test</Label>);
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('Label');
  });

  it('renders children', () => {
    expect(wrapper.children().text()).toBe('test');
  });
});
