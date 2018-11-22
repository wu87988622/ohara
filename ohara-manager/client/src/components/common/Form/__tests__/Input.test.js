import React from 'react';
import { shallow } from 'enzyme';

import Input from '../Input';

const props = {
  value: 'test input',
  handleChange: jest.fn(),
  placeholder: 'test',
};

describe('<Input />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Input {...props} />);
  });

  it('renders self', () => {
    const input = wrapper.find('Input');
    const {
      type,
      value,
      placeholder,
      width,
      height,
      disabled,
    } = wrapper.props();

    expect(input.length).toBe(1);
    expect(type).toBe('text');
    expect(value).toBe(props.value);
    expect(placeholder).toBe(props.placeholder);
    expect(width).toBe('120px');
    expect(height).toBe('32px');
    expect(disabled).toBe(false);
  });

  it('renders disable class when disabled is set to true', () => {
    wrapper.setProps({ disabled: true });
    expect(wrapper.props().className).toMatch(/is-disabled/);
  });

  it('handles change', () => {
    const evt = { e: { target: { value: 'b' } } };

    expect(props.handleChange).toHaveBeenCalledTimes(0);
    wrapper.simulate('change', evt);
    expect(props.handleChange).toHaveBeenCalledTimes(1);
    expect(props.handleChange).toBeCalledWith(evt);
  });
});
