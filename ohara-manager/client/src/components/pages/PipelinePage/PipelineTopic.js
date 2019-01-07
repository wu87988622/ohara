import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Facebook } from 'react-content-loader';

import * as _ from 'utils/commonUtils';
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

class PipelineTopic extends React.Component {
  static propTypes = {
    name: PropTypes.string.isRequired,
    isLoading: PropTypes.bool.isRequired,
    updateGraph: PropTypes.func,
    loadGraph: PropTypes.func,
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  state = {
    topic: {},
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
          <Facebook style={{ width: '70%', height: 'auto' }} />
        ) : (
          <React.Fragment>
            <H5Wrapper>Topic : {topic.name}</H5Wrapper>
          </React.Fragment>
        )}
      </Box>
    );
  }
}

export default PipelineTopic;
