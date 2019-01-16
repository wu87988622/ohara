import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import { Facebook } from 'react-content-loader';

import { Box } from 'common/Layout';
import { Select } from 'common/Form';
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
    topics: PropTypes.array.isRequired,
    currentTopic: PropTypes.object.isRequired,
    isLoading: PropTypes.bool.isRequired,
    updateTopic: PropTypes.func.isRequired,
  };

  handleSelectChange = ({ target }) => {
    const selectedIdx = target.options.selectedIndex;
    const { id } = target.options[selectedIdx].dataset;
    const currentTopic = { name: target.value, id };

    this.props.updateTopic(currentTopic);
  };

  update = () => {
    const { updateGraph, graph, currentTopic } = this.props;

    if (!currentTopic) {
      return toastr.error('Please select a topic!');
    }

    update({ graph, updateGraph, connector: currentTopic });
  };

  render() {
    const { isLoading, topics, currentTopic } = this.props;

    if (!topics) return null;

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
