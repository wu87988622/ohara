import React from 'react';
import { shallow } from 'enzyme';

import App from '../App';
import localStorageMock from '../__mocks__/localStorage';

window.localStorage = localStorageMock;

describe('<App />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<App />);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
  });
});
