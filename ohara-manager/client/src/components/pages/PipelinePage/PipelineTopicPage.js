import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Box } from '../../common/Layout';
import { H5 } from '../../common/Heading';
import { lightBlue } from '../../../theme/variables';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

const PipelineTopicPage = ({ name }) => {
  return (
    <Box>
      <H5Wrapper>Topic : {name}</H5Wrapper>
      <H5Wrapper>Schema</H5Wrapper>
    </Box>
  );
};

PipelineTopicPage.propTypes = {
  name: PropTypes.string.isRequired,
};

export default PipelineTopicPage;
