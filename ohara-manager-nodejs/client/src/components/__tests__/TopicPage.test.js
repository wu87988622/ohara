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

  it('renders <Header />', () => {
    expect(wrapper.find('Header').length).toBe(1);
  });

  it('renders content correctly', () => {
    expect(wrapper.find('[data-test="content"]').length).toBe(1);
    expect(wrapper.find('[data-test="content"]').text()).toBe(
      'See the topics here!',
    );
  });
});
