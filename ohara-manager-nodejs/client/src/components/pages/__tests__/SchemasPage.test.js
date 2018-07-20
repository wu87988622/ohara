import React from 'react';
import { shallow } from 'enzyme';

import SchemasPage from '../SchemasPage';

describe('<SchemasPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<SchemasPage />);
    wrapper.setState({ isLoading: false });
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Schemas');
  });

  // TODO: finish all the tests
});
