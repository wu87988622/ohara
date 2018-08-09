import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import { Prompt } from 'react-router-dom';

import Modal from './Modal';
import {
  validate,
  save,
  createTopics,
  fetchTopics,
} from '../../../apis/kafkaApis';
import { Input, Button } from '../../common/Form';
import { ListTable } from '../../common/Table';
import { submitButton, cancelButton } from '../../../theme/buttonTheme';
import { get, isDefined } from '../../../utils/helpers';
import {
  LEAVE_WITHOUT_SAVE,
  TOPIC_CREATION_SUCCESS,
} from '../../../constants/message';
import {
  white,
  lightBlue,
  lighterGray,
  radiusNormal,
  shadowNormal,
} from '../../../theme/variables';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Section = styled.section`
  background-color: ${white};
  border-radius: ${radiusNormal};
  box-shadow: ${shadowNormal};
  margin-bottom: 20px;
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

const Actions = styled.div`
  display: flex;
  padding: 20px;
  border-top: 1px solid ${lighterGray};
`;

const ActionGroup = styled.div`
  margin-left: auto;
`;

const Label = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
`;

const CancelButton = styled(Button)`
  margin-right: 10px;
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
    target: 'hdfs',
    clusterName: '',
    brokerList: '',
    isFormDirty: false,
    tableHeaders: ['Topic name', 'Details link'],
    isModalActive: false,
    topicName: '',
    partitions: '',
    replicationFactor: '',
    topics: [],
    isTestConnectionWorking: false,
    isSaveWorking: false,
    isCreateTopicWorking: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await fetchTopics();

    const isSuccess = get(res, 'data.isSuccess', undefined);

    if (isSuccess) {
      const { uuids } = res.data;
      const topics = Object.keys(uuids).map(key => {
        return {
          [key]: uuids[key],
        };
      });

      this.setState({ topics, isLoading: false });
    }
  };

  handleModalOpen = () => {
    this.setState({ isModalActive: true });
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false });
    this.resetModal();
  };

  handleCreateTopics = async e => {
    e.preventDefault();
    const {
      topicName: name,
      partitions: numberOfPartitions,
      replicationFactor: numberOfReplications,
    } = this.state;

    this.setState({ isCreateTopicWorking: true });
    const res = await createTopics({
      name,
      numberOfPartitions,
      numberOfReplications,
    });
    this.setState({ isCreateTopicWorking: false });

    const result = get(res, 'data.isSuccess', undefined);

    if (result) {
      toastr.success(TOPIC_CREATION_SUCCESS);
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

  handleSaveConfigs = async e => {
    e.preventDefault();
    const { target, clusterName, brokerList } = this.state;
    this.setState({ isSaveConfigsWorking: true });
    const res = await save({ target, clusterName, brokerList });
    this.setState({ isSaveConfigsWorking: false });

    const result = get(res, 'data.isSuccess', undefined);

    if (isDefined(result)) {
      toastr.success('Kafka configurations saved!');
    }
  };

  handleTest = async e => {
    e.preventDefault();
    const { target, clusterName, brokerList } = this.state;
    this.setState({ isTestConnectionWorking: true });
    const res = await validate({ target, clusterName, url: brokerList });
    this.setState({ isTestConnectionWorking: false });

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success('Test passed!');
    }
  };

  resetModal = () => {
    this.setState({ topicName: '', partitions: '', replicationFactor: '' });
  };

  render() {
    const {
      clusterName,
      brokerList,
      isFormDirty,
      tableHeaders,
      isModalActive,
      topicName,
      topics,
      partitions,
      replicationFactor,
      isTestConnectionWorking,
      isSaveConfigsWorking,
      isCreateTopicWorking,
    } = this.state;

    return (
      <Wrapper>
        <Prompt when={isFormDirty} message={LEAVE_WITHOUT_SAVE} />
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
        <h2>Kafka</h2>

        <Section>
          <form>
            <FormInner>
              <FormGroup>
                <Label>Cluster name</Label>
                <Input
                  type="text"
                  width="250px"
                  id="clusterName"
                  placeholder="kafka-cluster"
                  value={clusterName}
                  data-testid="clusterName"
                  handleChange={this.handleChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>Broker List</Label>
                <Input
                  type="text"
                  width="250px"
                  id="brokerList"
                  placeholder="http://localhost:5050"
                  value={brokerList}
                  data-testid="brokerList"
                  handleChange={this.handleChange}
                />
              </FormGroup>
            </FormInner>

            <Actions>
              <Button
                text="Test connection"
                handleClick={this.handleTest}
                isWorking={isTestConnectionWorking}
                data-testid="testConnection"
              />
              <ActionGroup>
                <CancelButton
                  text="Cancel"
                  theme={cancelButton}
                  data-testid="cancelButton"
                  handleClick={this.handleCancel}
                />
                <Button
                  text="Save"
                  isWorking={isSaveConfigsWorking}
                  theme={submitButton}
                  handleClick={this.handleSaveConfigs}
                />
              </ActionGroup>
            </Actions>
          </form>
        </Section>

        <Section>
          <TopicsInner>
            <SectionHeader>
              <H3>Topics</H3>
              <Button
                text="New topic"
                theme={submitButton}
                data-testid="newTopic"
                handleClick={this.handleModalOpen}
              />
            </SectionHeader>

            <ListTable headers={tableHeaders} list={topics} />
          </TopicsInner>
        </Section>
      </Wrapper>
    );
  }
}

export default KafkaPage;
