import React from 'react';
import styled from 'styled-components';
import { Facebook } from 'react-content-loader';

import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lighterBlue, lightBlue, durationNormal, blue } from 'theme/variables';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

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
  static propTypes = {};

  state = {
    isLoading: true,
  };

  update = () => {
    // TODO:
  };

  render() {
    const { isLoading } = this.state;

    return (
      <Box shadow={false}>
        {isLoading ? (
          <Facebook style={{ width: '70%', height: 'auto' }} />
        ) : (
          <React.Fragment>
            <H5Wrapper>New Topic</H5Wrapper>
          </React.Fragment>
        )}
      </Box>
    );
  }
}

export default PipelineNewTopic;
