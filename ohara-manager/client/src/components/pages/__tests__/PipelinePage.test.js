import React from 'react';
import { shallow } from 'enzyme';

import PipelinePage from '../PipelinePage';

describe('<PipelinePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelinePage />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Pipeline');
  });
});
