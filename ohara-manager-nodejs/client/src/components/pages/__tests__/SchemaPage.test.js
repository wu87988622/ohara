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
    expect(wrapper.props().title).toBe('Schema');
  });
});
