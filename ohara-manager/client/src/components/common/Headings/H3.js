import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const H3Wrapper = styled.h3`
  font-size: 22px;
`;

H3Wrapper.displayName = 'H3Wrapper';

const H3 = ({ children, ...rest }) => {
  return <H3Wrapper {...rest}>{children}</H3Wrapper>;
};

H3.propTypes = {
  children: PropTypes.any,
};

export default H3;
