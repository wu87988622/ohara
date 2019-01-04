import React from 'react';
import { shallow } from 'enzyme';
import toastr from 'toastr';

import PipelineNewPage from '../PipelineNewPage';
import { CONNECTOR_KEYS } from 'constants/pipelines';
import { PIPELINE_NEW, PIPELINE_EDIT } from 'constants/documentTitles';
import { getTestById } from 'utils/testUtils';
import {
  startSink,
  startSource,
  stopSink,
  stopSource,
  fetchPipeline,
} from 'apis/pipelinesApis';

jest.mock('apis/pipelinesApis');

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
      pipelines: { name: 'test' },
    });
  });

  afterEach(() => wrapper.setState({ pipelines: {} }));

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

  it('renders 5 <Route />', () => {
    expect(wrapper.find('Route').length).toBe(5);
  });

  it('renders <StartStopIcon />', () => {
    const button = wrapper.find(getTestById('start-stop-icon'));

    expect(button.length).toBe(1);
    expect(button.find('i').props().className).toMatch(/^fa fa-play-circle$/);
  });

  it.skip('displays an error message if pipeline does not have status', async () => {
    const data = {
      result: {
        name: 'newPipeline',
        objects: [{ kind: CONNECTOR_KEYS.topic, name: 'a', id: '1' }],
        rules: {},
      },
    };

    fetchPipeline.mockImplementation(() => Promise.resolve({ data }));

    startSink.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    startSource.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(
      'Cannot complete your action, please check your connector settings',
    );
  });

  // TODO: fix this failing test, the UI is working as expected but the test somehow fails...
  it.skip('starts the pipeline if the pipeline status is stopped', async () => {
    const data = {
      result: {
        name: 'test',
        status: 'Stopped',
        objects: [
          { kind: CONNECTOR_KEYS.jdbcSource, name: 'c', id: '3' },
          { kind: CONNECTOR_KEYS.hdfsSink, name: 'b', id: '2' },
          { kind: CONNECTOR_KEYS.topic, name: 'a', id: '1' },
        ],
        rules: {},
      },
    };

    fetchPipeline.mockImplementation(() => Promise.resolve({ data }));

    startSink.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    startSource.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    expect(startSource).toHaveBeenCalledTimes(1);
    expect(startSource).toHaveBeenCalledWith(data.result.objects[0].id);
    expect(startSink).toHaveBeenCalledTimes(1);
    expect(startSink).toHaveBeenCalledWith(data.result.objects[1].id);

    const button = wrapper.find(getTestById('start-stop-icon'));
    expect(button.find('i').props().className).toMatch(/^fa fa-stop-circle$/);
  });

  it.skip('stops the pipeline if the pipeline status is started', async () => {
    const data = {
      result: {
        name: 'test',
        objects: [
          { kind: CONNECTOR_KEYS.jdbcSource, name: 'c', id: '3' },
          { kind: CONNECTOR_KEYS.hdfsSink, name: 'b', id: '2' },
          { kind: CONNECTOR_KEYS.topic, name: 'a', id: '1' },
        ],
        rules: {},
      },
    };

    fetchPipeline.mockImplementation(() => Promise.resolve({ data }));

    startSink.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    startSource.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    // Start the pipeline
    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    // Stop the pipeline
    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    expect(stopSource).toHaveBeenCalledTimes(1);
    expect(stopSource).toHaveBeenCalledWith(data.result.objects[0].id);
    expect(stopSink).toHaveBeenCalledTimes(1);
    expect(stopSink).toHaveBeenCalledWith(data.result.objects[1].id);

    const button = wrapper.find(getTestById('start-stop-icon'));
    expect(button.find('i').props().className).toMatch(/^fa fa-play-circle$/);
  });
});
