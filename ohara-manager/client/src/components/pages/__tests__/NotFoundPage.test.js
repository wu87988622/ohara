import React from 'react';
import { shallow } from 'enzyme';

import NotFoundPage from '../NotFoundPage';
import { NOT_FOUND_PAGE } from 'constants/documentTitles';

describe('<NotFoundPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<NotFoundPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(NOT_FOUND_PAGE);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe(
      'Oops, page not found',
    );
  });
});
