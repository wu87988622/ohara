import React from 'react';
import { shallow } from 'enzyme';

import DeploymentPage from '../DeploymentPage';
import { DEPLOYMENT } from 'constants/documentTitles';

describe('<DeploymentPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<DeploymentPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(DEPLOYMENT);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Deployment');
  });
});
