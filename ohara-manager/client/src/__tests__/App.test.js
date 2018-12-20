import React from 'react';
import { shallow } from 'enzyme';

import App from '../App';
import localStorageMock from '../__mocks__/localStorage';
import { getUserKey } from 'utils/authUtils';
import { getTestById } from 'utils/testUtils';

jest.mock('utils/authUtils');

window.localStorage = localStorageMock;

describe('<App />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<App />);
    wrapper.setState({ isLogin: false });
    jest.clearAllMocks();
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('BrowserRouter');
  });

  it('renders <Header />', () => {
    const header = wrapper.find('Header');
    const _props = header.props();

    expect(header.length).toBe(1);
    expect(_props.isLogin).toBe(wrapper.state().isLogin);
    expect(_props.isLogin).toBe(false);
  });

  it('renders <Switch />', () => {
    expect(wrapper.find('Switch').length).toBe(1);
  });

  it('renders pipeline new page route', () => {
    // TODO: reuse routes config in App.js
    const routes = [
      {
        path: '/pipelines/new/:page?/:pipelineId/:connectorId?',
        testId: 'pipeline-new-page',
      },
      {
        path: '/pipelines/edit/:page?/:pipelineId/:connectorId?',
        testId: 'pipeline-edit-page',
      },
      {
        path: '/pipelines',
        testId: 'pipeline-page',
      },
      {
        path: '/configuration',
        testId: 'configuration-page',
      },
      {
        path: '/deployment',
        testId: 'deployment-page',
      },
      {
        path: '/monitoring',
        testId: 'monitoring-page',
      },
      {
        path: '/login',
        testId: 'login-page',
      },
      {
        path: '/logout',
        testId: 'logout-page',
      },
      {
        path: '/',
        testId: 'home-page',
      },
      {
        testId: 'not-found-page',
      },
    ];
    routes.forEach(({ testId }, idx) => {
      const currRoute = wrapper.find(getTestById(testId));
      expect(currRoute.length).toBe(1);
      expect(currRoute.props().path).toBe(routes[idx].path);
    });
  });

  it('does not change login status in <Header />, if user is not logged in', () => {
    getUserKey.mockReturnValue(undefined);

    wrapper = shallow(<App />);
    expect(getUserKey).toHaveBeenCalledTimes(1);
    expect(wrapper.find('Header').props().isLogin).toBe(false);
  });

  it('Changes login status in <Header />, if user is logged in', () => {
    getUserKey.mockReturnValue('12345');
    wrapper = shallow(<App />);

    expect(getUserKey).toHaveBeenCalledTimes(1);
    expect(wrapper.find('Header').props().isLogin).toBe(true);
  });

  it('updates <Header /> isLogin prop correctly', () => {
    expect(wrapper.find('Header').props().isLogin).toBe(false);

    wrapper.instance().updateLoginState(true);
    expect(wrapper.find('Header').props().isLogin).toBe(true);

    wrapper.instance().updateLoginState(false);
    expect(wrapper.find('Header').props().isLogin).toBe(false);
  });
});
