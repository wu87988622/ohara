import React from 'react';
import { shallow } from 'enzyme';

import NotFoundPage from '../NotFoundPage';

describe('<NotFoundPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<NotFoundPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe(
      'Oops, page not found',
    );
  });
});
