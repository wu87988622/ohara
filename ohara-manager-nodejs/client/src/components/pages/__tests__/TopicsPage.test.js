import React from 'react';
import { shallow } from 'enzyme';

import TopicsPage from '../TopicsPage';
import Loading from '../../common/Loading';

import * as api from '../../../apis/topicApi';

const res = { data: { uuids: { a: 'a', b: 'b' } } };

api.fetchTopics = jest.fn(() => {
  return res;
});

describe('<TopicsPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<TopicsPage />);
    wrapper.setState({ isLoading: false });
  });

  it('renders <Loading />', () => {
    wrapper.setState({ isLoading: true });
    expect(wrapper.type()).toBe(Loading);
  });

  it('renders correctly', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.props().title).toBe('Topics');
  });

  it('renders <table />', () => {
    const table = wrapper.find('table');
    expect(table.length).toBe(1);
    expect(table.hasClass('table table-striped')).toBe(true);
  });

  it('renders correct <th />', () => {
    const headerLength = wrapper.state().headers.length;
    expect(wrapper.find('th').length).toBe(headerLength);
  });
});
