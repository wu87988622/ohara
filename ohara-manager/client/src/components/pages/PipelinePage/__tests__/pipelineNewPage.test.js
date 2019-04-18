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

import PipelineNewPage from '../PipelineNewPage';
import { CONNECTOR_TYPES } from 'constants/pipelines';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';
import { getTestById } from 'utils/testUtils';
import * as pipelineApi from 'api/pipelineApi';
import * as connectorApi from 'api/connectorApi';

jest.mock('api/pipelineApi');
jest.mock('api/connectorApi');

const props = {
  match: {
    params: {
      topicId: '1234',
      pipelineId: '5678',
    },
  },
};

describe('<PipelineNewPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<PipelineNewPage {...props} />);

    jest.clearAllMocks();

    // TODO: change this to a more real world like case, e.g., mock data returns by some requests
    wrapper.setState({
      pipeline: { name: 'test', workerClusterName: 'abc' },
    });
  });

  afterEach(() => wrapper.setState({ pipeline: {} }));

  it('renders self', () => {
    expect(wrapper.find('Wrapper').length).toBe(1);
  });

  it('renders new pipeline page document title', () => {
    const match = {
      params: {
        pipelineId: null,
      },
    };
    wrapper.setProps({ match });

    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.props().title).toBe(PIPELINE_NEW);
  });

  it('renders edit pipeline page document title, if pipelineId is present', () => {
    expect(wrapper.props().title).toBe(PIPELINE_EDIT);
  });

  it('renders the <H2 />', () => {
    expect(wrapper.find('H2').length).toBe(1);
  });

  it('renders <Toolbar />', () => {
    expect(wrapper.find('PipelineToolbar').length).toBe(1);
  });

  it('renders <PipelineGraph />', () => {
    expect(wrapper.find('PipelineGraph').length).toBe(1);
  });

  it('renders 7 <Route />', () => {
    expect(wrapper.find('Route').length).toBe(7);
  });

  it('renders <StartBtn />', () => {
    const button = wrapper.find(getTestById('start-btn'));

    expect(button.length).toBe(1);
    expect(button.find('i').props().className).toMatch(/^far fa-play-circle$/);
  });

  it('renders <StopBtn />', () => {
    const button = wrapper.find(getTestById('stop-btn'));

    expect(button.length).toBe(1);
    expect(button.find('i').props().className).toMatch(/^far fa-stop-circle$/);
  });

  it.skip('starts the pipeline', async () => {
    const data = {
      result: {
        name: 'test',
        status: 'Stopped',
        objects: [
          { kind: CONNECTOR_TYPES.jdbcSource, name: 'c', id: '3' },
          { kind: CONNECTOR_TYPES.hdfsSink, name: 'b', id: '2' },
          { kind: CONNECTOR_TYPES.topic, name: 'a', id: '1' },
        ],
        rules: {},
      },
    };

    pipelineApi.fetchPipeline.mockImplementation(() =>
      Promise.resolve({ data }),
    );

    connectorApi.startConnector.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    await wrapper.find(getTestById('start-btn')).prop('onClick')();

    expect(connectorApi.startConnector).toHaveBeenCalledTimes(2);
    expect(connectorApi.startConnector).toHaveBeenCalledWith(
      data.result.objects[0].id,
    );
    expect(connectorApi.startConnector).toHaveBeenCalledWith(
      data.result.objects[1].id,
    );
  });

  it.skip('stops the pipeline', async () => {
    const data = {
      result: {
        name: 'test',
        objects: [
          { kind: CONNECTOR_TYPES.jdbcSource, name: 'c', id: '3' },
          { kind: CONNECTOR_TYPES.hdfsSink, name: 'b', id: '2' },
          { kind: CONNECTOR_TYPES.topic, name: 'a', id: '1' },
        ],
        rules: {},
      },
    };

    pipelineApi.fetchPipeline.mockImplementation(() =>
      Promise.resolve({ data }),
    );

    connectorApi.stopConnector.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    // Stop the pipeline
    await wrapper.find(getTestById('stop-btn')).prop('onClick')();

    expect(connectorApi.stopConnector).toHaveBeenCalledTimes(2);
    expect(connectorApi.stopConnector).toHaveBeenCalledWith(
      data.result.objects[0].id,
    );
    expect(connectorApi.stopConnector).toHaveBeenCalledWith(
      data.result.objects[1].id,
    );
  });
});
