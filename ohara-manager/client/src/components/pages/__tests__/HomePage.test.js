import React from 'react';
import { shallow } from 'enzyme';

import HomePage from '../HomePage';
import { HOME } from 'constants/documentTitles';

describe('<HomePage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<HomePage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(HOME);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Ohara home');
  });
});
