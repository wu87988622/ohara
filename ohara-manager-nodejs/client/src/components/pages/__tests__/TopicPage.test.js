import React from 'react';
import { shallow } from 'enzyme';

import TopicPage from '../TopicPage';

describe('<TopicPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<TopicPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
  });
});
