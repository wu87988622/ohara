import React from 'react';
import { shallow } from 'enzyme';

import Select from '../Select';

const props = {
  list: [{ name: 'a', uuid: '1' }, { name: 'b', uuid: '2' }],
  selected: { name: 'a', uuid: '2' },
  handleChange: jest.fn(),
};

describe('<Select />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<Select {...props} />);
  });

  it('renders self', () => {
    expect(wrapper).toHaveLength(1);
  });

  it('renders the correct selected item', () => {
    expect(wrapper.props().value).toBe(props.selected.name);
  });

  it('renders list items correctly', () => {
    expect(wrapper.find('option').length).toBe(2);

    expect(
      wrapper.find('option').forEach((option, idx) => {
        expect(option.text()).toBe(props.list[idx].name);
      }),
    );
  });

  it('handles onChange', () => {
    const evt = { e: { target: { value: 'b' } } };

    expect(props.handleChange).toHaveBeenCalledTimes(0);

    wrapper.simulate('change', evt);
    expect(props.handleChange).toHaveBeenCalledTimes(1);
    expect(props.handleChange).toBeCalledWith(evt);
  });
});
