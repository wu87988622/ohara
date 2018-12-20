import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Facebook } from 'react-content-loader';

import * as _ from 'utils/commonUtils';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';
import { fetchPipeline } from 'apis/pipelinesApis';

const H5Wrapper = styled(H5)`
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

  componentDidMount() {
    const pipelineId = _.get(this.props.match, 'params.pipelineId', null);
    if (pipelineId) {
      this.fetchPipeline(pipelineId);
    }
  }

  fetchPipeline = async pipelineId => {
    if (!pipelineId) return;

    const res = await fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', []);

    if (!_.isEmpty(pipelines)) {
      this.props.loadGraph(pipelines);
    }
  };

  render() {
    const { name, isLoading } = this.props;
    return (
      <Box>
        {isLoading ? (
          <Facebook style={{ width: '70%', height: 'auto' }} />
        ) : (
          <React.Fragment>
            <H5Wrapper>Topic : {name}</H5Wrapper>
          </React.Fragment>
        )}
      </Box>
    );
  }
}

export default PipelineTopic;
