import React from 'react';
import { shallow } from 'enzyme';

import SchemasPage from '../SchemasPage';

describe('<SchemasPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<SchemasPage />);
    wrapper.setState({ isLoading: false });
  });

  // TODO: Skip the test now, since the feature is not confirm yet
  it.skip('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Schemas');
  });
});
