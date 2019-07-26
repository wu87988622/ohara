/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import { shallow } from 'enzyme';

import App from '../App';
import { getTestById } from 'utils/testUtils';

describe('<App />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<App />);
    jest.clearAllMocks();
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
    expect(wrapper.name()).toBe('StylesProvider');
  });

  it('renders <Header />', () => {
    const header = wrapper.find('Header');
    expect(header.length).toBe(1);
  });

  it('renders <Switch />', () => {
    expect(wrapper.find('Switch').length).toBe(1);
  });

  it('renders route', () => {
    const routes = [
      {
        path: '/pipelines/new/:page?/:pipelineName/:connectorName?',
        testId: 'pipeline-new-page',
      },
      {
        path: '/pipelines/edit/:page?/:pipelineName/:connectorName?',
        testId: 'pipeline-edit-page',
      },
      {
        path: '/pipelines',
        testId: 'pipeline-page',
      },
      {
        path: '/nodes',
        testId: 'nodes-page',
      },
      {
        path: '/workspaces/:workspaceName/:serviceName?',
        testId: 'workspace-page',
      },
      {
        path: '/workspaces',
        testId: 'workspaces-page',
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
});
