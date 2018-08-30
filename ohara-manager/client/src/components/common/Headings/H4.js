import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const H4Wrapper = styled.h4`
  font-size: 20px;
`;

H4Wrapper.displayName = 'H4Wrapper';

const H5 = ({ children, ...rest }) => {
  return <H4Wrapper {...rest}>{children}</H4Wrapper>;
};

H5.propTypes = {
  children: PropTypes.any,
};

export default H5;
