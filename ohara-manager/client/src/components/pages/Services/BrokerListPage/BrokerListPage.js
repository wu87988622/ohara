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
import { isEmpty, join, map, get, orderBy } from 'lodash';

import * as brokerApi from 'api/brokerApi';
import * as topicApi from 'api/topicApi';
import { Box } from 'common/Layout';
import { H2 } from 'common/Headings';
import { TableLoader } from 'common/Loader';
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
    const res = await brokerApi.fetchBrokers();
    const brokers = get(res, 'data.result', []);
    if (!isEmpty(brokers)) {
      this.setState({ broker: brokers[0] });
    }
  };

  fetchTopics = async () => {
    const res = await topicApi.fetchTopics();
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
