import React from 'react';
import { shallow } from 'enzyme';

import NodesPage from '../NodesPage';
import { NODES } from 'constants/documentTitles';

describe('<NodesPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<NodesPage />);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(NODES);
  });

  it('renders <AppWrapper />', () => {
    expect(wrapper.find('AppWrapper').length).toBe(1);
    expect(wrapper.find('AppWrapper').props().title).toBe('Nodes');
  });
});
