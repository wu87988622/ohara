import React from 'react';
import { shallow } from 'enzyme';

import KafkaPage from '../KafkaPage';

describe('<KafkaPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<KafkaPage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Kafka');
  });
});
