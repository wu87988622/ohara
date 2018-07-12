import React from 'react';
import { shallow } from 'enzyme';

import SchemaPage from '../SchemaPage';

describe('<SchemaPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<SchemaPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.find('.SchemaPage').length).toBe(1);
  });

  test('renders <Header />', () => {
    expect(wrapper.find('Header').length).toBe(1);
  });

  it('toggles the class isActive', () => {
    const page = wrapper.find('.SchemaPage');
    expect(page.hasClass('isActive')).toBe(true);

    page.simulate('click');
    expect(wrapper.find('.SchemaPage').hasClass('isActive')).not.toBe(true);
  });
});
