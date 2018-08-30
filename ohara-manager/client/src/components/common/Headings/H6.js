import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const H6Wrapper = styled.h6`
  font-size: 16px;
`;

H6Wrapper.displayName = 'H6Wrapper';

const H6 = ({ children, ...rest }) => {
  return <H6Wrapper {...rest}>{children}</H6Wrapper>;
};

H6.propTypes = {
  children: PropTypes.any,
};

export default H6;
