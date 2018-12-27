import React from 'react';
import { shallow } from 'enzyme';

import ServicesPage from '../ServicesPage';
import { SERVICES } from 'constants/documentTitles';

describe('<ServicesPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<ServicesPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(SERVICES);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Services');
  });
});
