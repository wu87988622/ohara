import React from 'react';
import { shallow } from 'enzyme';

import MonitoringPage from '../MonitoringPage';
import { MONITORING } from 'constants/documentTitles';

describe('<MonitoringPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<MonitoringPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(MONITORING);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Monitoring');
  });
});
