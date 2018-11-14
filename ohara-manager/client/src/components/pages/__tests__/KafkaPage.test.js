import React from 'react';
import toastr from 'toastr';
import { shallow } from 'enzyme';

import KafkaPage from '../KafkaPage';
import * as MESSAGES from 'constants/messages';
import { getTestById } from 'utils/testUtils';
import { LEAVE_WITHOUT_SAVE } from 'constants/messages';
import { KAFKA } from 'constants/documentTitles';
import { createTopics, fetchTopics } from 'apis/topicApis';
import { fetchCluster } from 'apis/clusterApis';

jest.mock('apis/topicApis');
jest.mock('apis/clusterApis');

const props = {
  history: {
    push: jest.fn(),
  },
};

describe('<KafkaPage />', () => {
  let wrapper;
  beforeEach(() => {
    wrapper = shallow(<KafkaPage {...props} />);
    jest.clearAllMocks();
  });

  it('renders self', () => {
    expect(wrapper.length).toBe(1);
  });

  it('renders <DocumentTitle />', () => {
    expect(wrapper.dive().name()).toBe('DocumentTitle');
    expect(wrapper.dive().props().title).toBe(KAFKA);
  });

  it('renders <Modal />', () => {
    const modal = wrapper.find('Modal');
    const _props = modal.props();
    expect(modal.length).toBe(1);
    expect(_props.isActive).toBe(false);
  });

  it('opens <Modal />', () => {
    expect(wrapper.find('Modal').props().isActive).toBe(false);
    wrapper.find(getTestById('new-topic')).prop('handleClick')();

    expect(wrapper.find('Modal').props().isActive).toBe(true);
  });

  it('closes <Modal /> with <CloseBtn>', () => {
    wrapper.setState({ isModalActive: true });

    expect(wrapper.find('Modal').props().isActive).toBe(true);
    wrapper.find('Modal').prop('handleClose')();
    expect(wrapper.find('Modal').props().isActive).toBe(false);
  });

  it('renders <Prompt />', () => {
    const prompt = wrapper.find('Prompt');
    const _props = prompt.props();

    expect(prompt.length).toBe(1);
    expect(_props.when).toBe(false);
    expect(_props.message).toBe(LEAVE_WITHOUT_SAVE);
  });

  it('enables the <Prompt /> when the form has been edited', () => {
    expect(wrapper.find('Prompt').props().when).toBe(false);
    wrapper.find('Modal').prop('handleChange')({
      target: { id: '123', value: 'testValue' },
    });
    expect(wrapper.find('Prompt').props().when).toBe(true);
  });

  it('creates a new topic', async () => {
    const evt = { preventDefault: jest.fn() };
    const createTopicRes = { data: { isSuccess: true } };
    const fetchClusterRes = { data: { isSuccess: true } };
    const fetchTopicRes = { data: { isSuccess: true, result: ['a', 'b'] } };
    const params = {
      topicName: 'topic',
      partitions: '1',
      replicationFactor: '2',
    };
    createTopics.mockImplementation(() => Promise.resolve(createTopicRes));

    wrapper.setState({ ...params, isModalActive: true });
    expect(wrapper.find('Modal').props().isActive).toBe(true);
    expect(wrapper.find('ListTable').props().list).toEqual([]);

    fetchTopics.mockImplementation(() => Promise.resolve(fetchTopicRes));
    fetchCluster.mockImplementation(() => Promise.resolve(fetchClusterRes));

    await wrapper.find('Modal').prop('handleCreate')(evt);

    expect(createTopics).toHaveBeenCalledTimes(1);
    expect(createTopics).toBeCalledWith({
      name: params.topicName,
      numberOfPartitions: Number(params.partitions),
      numberOfReplications: Number(params.replicationFactor),
    });
    expect(toastr.success).toHaveBeenCalledTimes(1);
    expect(toastr.success).toBeCalledWith(MESSAGES.TOPIC_CREATION_SUCCESS);
    expect(wrapper.find('Modal').props().isActive).toBe(false);
    expect(fetchTopics).toHaveBeenCalledTimes(1);
    expect(wrapper.find('ListTable').props().list).toEqual(
      fetchTopicRes.data.result,
    );
  });
});
