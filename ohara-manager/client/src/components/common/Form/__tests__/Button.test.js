import React from 'react';
import { shallow } from 'enzyme';

import Button from '../Button';

const props = {
  text: 'test',
  handleClick: jest.fn(),
};

describe('<Button />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Button {...props} />);
  });

  it('renders self', () => {
    const _props = wrapper.props();
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('Button');
    expect(_props.type).toBe('submit');
    expect(_props.width).toBe('auto');
    expect(_props.disabled).toBe(false);
  });

  it('handles click', () => {
    expect(props.handleClick).toHaveBeenCalledTimes(0);

    wrapper.simulate('click');
    expect(props.handleClick).toHaveBeenCalledTimes(1);
  });

  it('adds is-working class name and <Icon /> when props.isWorking is true', () => {
    expect(wrapper.props().className.trim()).toBe('');
    expect(wrapper.find('Icon').length).toBe(0);

    wrapper = shallow(<Button {...props} isWorking />);
    expect(wrapper.find('Icon').length).toBe(1);
    expect(wrapper.props().className).toMatch(/is-working/);
  });

  it('adds is-disabled class name and disabled prop when props.disabled is true', () => {
    expect(wrapper.props().className.trim()).toBe('');

    wrapper = shallow(<Button {...props} disabled />);
    expect(wrapper.props().className).toMatch(/is-disabled/);
    expect(wrapper.props().disabled).toBe(true);
  });
});
