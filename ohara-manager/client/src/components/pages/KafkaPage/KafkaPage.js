import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import DocumentTitle from 'react-document-title';
import { Prompt } from 'react-router-dom';

import Modal from './Modal';
import { H2 } from '../../common/Headings';
import { fetchCluster } from '../../../apis/clusterApis';
import { createTopics, fetchTopics } from '../../../apis/topicApis';
import { Input, Button } from '../../common/Form';
import { ListTable } from '../../common/Table';
import { primaryBtn } from '../../../theme/btnTheme';
import { get } from '../../../utils/helpers';
import { KAFKA } from '../../../constants/documentTitles';
import { lightBlue } from '../../../theme/variables';
import { Box } from '../../common/Layout';
import * as MESSAGES from '../../../constants/messages';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const FormInner = styled.div`
  padding: 45px 30px;
`;

const TopicsInner = styled.div`
  padding: 30px;
`;

const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const Label = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
`;

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 25px;
`;

const H3 = styled.h3`
  margin: 0 30px 0 0;
`;

class KafkaPage extends React.Component {
  state = {
    brokerList: '',
    workerList: '',
    isFormDirty: false,
    tableHeaders: ['Topic name', 'Details link'],
    isModalActive: false,
    topicName: '',
    partitions: '',
    replicationFactor: '',
    topics: [],
    isCreateTopicWorking: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const topicRes = await fetchTopics();
    const clusterRes = await fetchCluster();

    const _topicResult = get(topicRes, 'data.result', null);
    const _clusterResult = get(clusterRes, 'data.result', null);

    if (_topicResult && _topicResult.length > 0) {
      this.setState({ topics: _topicResult, isLoading: false });
    }

    if (_clusterResult) {
      const { brokers: brokerList, workers: workerList } = _clusterResult;

      this.setState({ brokerList, workerList });
    }
  };

  handleModalOpen = () => {
    this.setState({ isModalActive: true });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false, isFormDirty: false });
    this.resetModal();
  };

  handleCreateTopics = async e => {
    e.preventDefault();
    const { topicName: name, partitions, replicationFactor } = this.state;

    this.setState({ isCreateTopicWorking: true });
    const res = await createTopics({
      name,
      numberOfPartitions: Number(partitions),
      numberOfReplications: Number(replicationFactor),
    });
    this.setState({ isCreateTopicWorking: false });

    const result = get(res, 'data.isSuccess', undefined);

    if (result) {
      toastr.success(MESSAGES.TOPIC_CREATION_SUCCESS);
      this.handleModalClose();
      this.fetchData();
    }
  };

  handleChange = ({ target: { id, value } }) => {
    this.setState({ [id]: value, isFormDirty: true });
  };

  handleCancel = e => {
    e.preventDefault();
    this.props.history.goBack();
  };

  resetModal = () => {
    this.setState({ topicName: '', partitions: '', replicationFactor: '' });
  };

  render() {
    const {
      brokerList,
      workerList,
      isFormDirty,
      tableHeaders,
      isModalActive,
      topicName,
      topics,
      partitions,
      replicationFactor,
      isCreateTopicWorking,
    } = this.state;

    return (
      <DocumentTitle title={KAFKA}>
        <Wrapper>
          <Prompt when={isFormDirty} message={MESSAGES.LEAVE_WITHOUT_SAVE} />
          <Modal
            isActive={isModalActive}
            topicName={topicName}
            partitions={partitions}
            replicationFactor={replicationFactor}
            handleChange={this.handleChange}
            handleCreate={this.handleCreateTopics}
            handleClose={this.handleModalClose}
            isCreateTopicWorking={isCreateTopicWorking}
          />
          <H2>Kafka</H2>

          <Box>
            <form>
              <FormInner>
                <FormGroup>
                  <Label>Broker List</Label>
                  <Input
                    width="350px"
                    value={brokerList}
                    data-testid="broker-list"
                    disabled
                  />
                </FormGroup>
                <FormGroup>
                  <Label>Worker List</Label>
                  <Input
                    width="350px"
                    value={workerList}
                    data-testid="worker-list"
                    disabled
                  />
                </FormGroup>
              </FormInner>
            </form>
          </Box>

          <Box>
            <TopicsInner>
              <SectionHeader>
                <H3>Topics</H3>
                <Button
                  text="New topic"
                  theme={primaryBtn}
                  data-testid="new-topic"
                  handleClick={this.handleModalOpen}
                />
              </SectionHeader>

              <ListTable headers={tableHeaders} list={topics} />
            </TopicsInner>
          </Box>
        </Wrapper>
      </DocumentTitle>
    );
  }
}

export default KafkaPage;
