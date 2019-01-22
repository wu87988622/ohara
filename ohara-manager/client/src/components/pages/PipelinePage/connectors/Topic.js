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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import * as _ from 'utils/commonUtils';
import { ListLoader } from 'common/Loader';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { fetchTopic } from 'apis/topicApis';

const H5Wrapper = styled(H5)`
  font-size: 15px;
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

class Topic extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  state = {
    topic: null,
    isLoading: true,
  };

  componentDidMount() {
    this.fetchTopic();
  }

  componentDidUpdate(prevProps) {
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevConnectorId !== currConnectorId) {
      this.fetchTopic();
    }
  }

  fetchTopic = async () => {
    const { connectorId } = this.props.match.params;
    const res = await fetchTopic(connectorId);
    const topic = _.get(res, 'data.result', null);

    if (topic) {
      this.setState({
        topic,
        isLoading: false,
      });
    }
  };

  render() {
    const { topic, isLoading } = this.state;
    return (
      <Box>
        {isLoading ? (
          <ListLoader />
        ) : (
          <React.Fragment>
            <H5Wrapper>Topic : {topic.name}</H5Wrapper>
          </React.Fragment>
        )}
      </Box>
    );
  }
}

export default Topic;
