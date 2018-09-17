import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Facebook } from 'react-content-loader';

import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue } from 'theme/variables';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

const PipelineTopicPage = ({ name, isLoading }) => {
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
};

PipelineTopicPage.propTypes = {
  name: PropTypes.string.isRequired,
  isLoading: PropTypes.bool.isRequired,
};

export default PipelineTopicPage;
