import React from 'react';
import { isEmpty, join, map, get, orderBy } from 'lodash';

import * as brokerApis from 'apis/brokerApis';
import * as topicApis from 'apis/topicApis';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import TableLoader from 'common/Loader';
import { FormGroup, Input, Label } from 'common/Form';
import { primaryBtn } from 'theme/btnTheme';

import TopicNewModal from '../TopicNewModal';
import * as s from './Styles';

class BrokerListPage extends React.Component {
  headers = ['TOPIC NAME', 'PARTITIONS', 'REPLICATION FACTOR'];

  state = {
    isLoading: true,
    broker: [],
    topics: [],
    isModalOpen: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const fetchBrokersPromise = this.fetchBrokers();
    const fetchTopicsPromise = this.fetchTopics();
    await Promise.all([fetchBrokersPromise, fetchTopicsPromise]);
    this.setState(() => ({ isLoading: false }));
  };

  fetchBrokers = async () => {
    const res = await brokerApis.fetchBrokers();
    const brokers = get(res, 'data.result', []);
    if (!isEmpty(brokers)) {
      this.setState({ broker: brokers[0] });
    }
  };

  fetchTopics = async () => {
    const res = await topicApis.fetchTopics();
    const topics = get(res, 'data.result', []);
    if (!isEmpty(topics)) {
      this.setState({ topics: orderBy(topics, 'name') });
    }
  };

  getNodeList = () => {
    const { broker } = this.state;
    if (!broker) return [];

    return map(broker.nodeNames, nodeName => {
      return `${nodeName}:${broker.clientPort}`;
    });
  };

  render() {
    const { isLoading, topics, isModalOpen } = this.state;
    return (
      <React.Fragment>
        {isLoading && (
          <Box>
            <FormGroup isInline>
              <H2>Services > Broker</H2>
            </FormGroup>
            <TableLoader />
          </Box>
        )}

        {!isLoading && (
          <Box>
            <FormGroup isInline>
              <H2>Services > Broker</H2>
            </FormGroup>
            <FormGroup isInline>
              <Label style={{ marginRight: '2rem' }}>Broker list</Label>
              <Input
                width="30rem"
                value={join(this.getNodeList(), ', ')}
                data-testid="broker-list"
                disabled
              />
            </FormGroup>
          </Box>
        )}

        {!isLoading && (
          <Box>
            <s.TopWrapper>
              <H2>Topics</H2>
              <s.NewBtn
                theme={primaryBtn}
                text="New topic"
                data-testid="new-topic"
                handleClick={() => {
                  this.setState({ isModalOpen: true });
                }}
              />
            </s.TopWrapper>
            <s.Table headers={this.headers}>
              {topics.map(topic => (
                <tr key={topic.id}>
                  <td>{topic.name}</td>
                  <td>{topic.numberOfPartitions}</td>
                  <td>{topic.numberOfReplications}</td>
                </tr>
              ))}
            </s.Table>
          </Box>
        )}

        <TopicNewModal
          isActive={isModalOpen}
          onClose={() => {
            this.setState({ isModalOpen: false });
          }}
          onConfirm={this.fetchTopics}
        />
      </React.Fragment>
    );
  }
}

export default BrokerListPage;
