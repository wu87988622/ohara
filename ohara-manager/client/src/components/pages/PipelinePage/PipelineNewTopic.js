import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { Facebook } from 'react-content-loader';

import { Box } from 'common/Layout';
import { Select } from 'common/Form';
import { fetchTopics } from 'utils/pipelineUtils';
import { lighterBlue, durationNormal, blue } from 'theme/variables';
import { update } from 'utils/pipelineToolbarUtils';

const Icon = styled.i`
  color: ${lighterBlue};
  font-size: 25px;
  margin-right: 20px;
  transition: ${durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${durationNormal} all;
    color: ${blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

Icon.displayName = 'Icon';

class PipelineNewTopic extends React.Component {
  static propTypes = {
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        id: PropTypes.string,
        isActive: PropTypes.bool,
        isExact: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    updateGraph: PropTypes.func.isRequired,
  };

  state = {
    isLoading: true,
    topics: [],
    currentTopic: {},
  };

  componentDidMount() {
    this.fetchTopics();
  }

  fetchTopics = async () => {
    const topics = await fetchTopics();
    if (topics) {
      this.setState({ topics, isLoading: false, currentTopic: topics[0] });
    }
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { uuid } = target.options[selectedIdx].dataset;

    this.setState({
      currentTopic: {
        name: target.value,
        uuid,
      },
    });
  };

  update = () => {
    const { updateGraph, graph } = this.props;
    const { currentTopic } = this.state;
    update({ graph, updateGraph, connector: currentTopic });
  };

  render() {
    const { isLoading, topics, currentTopic } = this.state;

    return (
      <Box shadow={false}>
        {isLoading ? (
          <Facebook style={{ width: '70%', height: 'auto' }} />
        ) : (
          <React.Fragment>
            <Select
              isObject
              list={topics}
              selected={currentTopic}
              handleChange={this.handleSelectChange}
            />
          </React.Fragment>
        )}
      </Box>
    );
  }
}

export default PipelineNewTopic;
