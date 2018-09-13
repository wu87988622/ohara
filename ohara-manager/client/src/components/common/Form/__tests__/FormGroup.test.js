import React from 'react';
import { shallow } from 'enzyme';

import FormGroup from '../FormGroup';

describe('<FormGroup />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(
      <FormGroup>
        <input />
      </FormGroup>,
    );
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('FormGroup');
  });

  it('renders children', () => {
    expect(wrapper.children().type()).toBe('input');
  });
});
