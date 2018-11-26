import React from 'react';
import uuid from 'uuid';
import { shallow } from 'enzyme';
import toastr from 'toastr';

import PipelineNewPage from '../PipelineNewPage';
import { ICON_KEYS } from 'constants/pipelines';
import { PIPELINE } from 'constants/urls';
import { PIPELINE_NEW } from 'constants/documentTitles';
import { getTestById } from 'utils/testUtils';
import {
  startSink,
  startSource,
  stopSink,
  stopSource,
} from 'apis/pipelinesApis';

jest.mock('apis/pipelinesApis');

const props = {
  match: {
    params: {
      topicId: uuid.v4(),
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

  it('renders self', () => {
    expect(wrapper.find('Wrapper').length).toBe(1);
  });

  it('renders the correct document title', () => {
    expect(wrapper.props().title).toBe(PIPELINE_NEW);
  });

  it('should render <Redirect /> when topicUuid is not present', () => {
    const match = {};
    wrapper = shallow(<PipelineNewPage match={match} />);
    expect(wrapper.props().to).toBe(PIPELINE);
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

  it('displays an error message if pipeline does not have status', () => {
    wrapper.find(getTestById('start-stop-icon')).prop('onClick')({});

    expect(toastr.error).toHaveBeenCalledTimes(1);
    expect(toastr.error).toHaveBeenCalledWith(
      'Failed to start the pipeline, please check your connectors settings',
    );
  });

  it('starts the pipeline if the pipeline status is stopped', async () => {
    const pipelines = {
      name: 'test',
      status: 'Stopped',
      objects: [
        { kind: ICON_KEYS.jdbcSource, name: 'b', uuid: '2' },
        { kind: ICON_KEYS.hdfsSink, name: 'b', uuid: '2' },
        { kind: ICON_KEYS.topic, name: 'a', uuid: '1' },
      ],
    };

    wrapper.setState({ pipelines });

    startSink.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    startSource.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    expect(startSource).toHaveBeenCalledTimes(1);
    expect(startSource).toHaveBeenCalledWith(pipelines.objects[0].uuid);
    expect(startSink).toHaveBeenCalledTimes(1);
    expect(startSource).toHaveBeenCalledWith(pipelines.objects[1].uuid);

    const button = wrapper.find(getTestById('start-stop-icon'));
    expect(button.find('i').props().className).toMatch(/^fa fa-stop-circle$/);
  });

  it('stops the pipeline if the pipeline status is started', async () => {
    const pipelines = {
      name: 'test',
      status: 'Running',
      objects: [
        { kind: ICON_KEYS.jdbcSource, name: 'b', uuid: '2' },
        { kind: ICON_KEYS.hdfsSink, name: 'b', uuid: '2' },
        { kind: ICON_KEYS.topic, name: 'a', uuid: '1' },
      ],
    };

    wrapper.setState({ pipelines });

    stopSink.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );
    stopSource.mockImplementation(() =>
      Promise.resolve({ data: { isSuccess: true } }),
    );

    await wrapper.find(getTestById('start-stop-icon')).prop('onClick')();

    expect(stopSource).toHaveBeenCalledTimes(1);
    expect(stopSource).toHaveBeenCalledWith(pipelines.objects[0].uuid);
    expect(stopSource).toHaveBeenCalledTimes(1);
    expect(stopSource).toHaveBeenCalledWith(pipelines.objects[1].uuid);

    const button = wrapper.find(getTestById('start-stop-icon'));
    expect(button.find('i').props().className).toMatch(/^fa fa-play-circle$/);
  });
});
