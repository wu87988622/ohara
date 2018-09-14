import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const H2Wrapper = styled.h2`
  font-weight: normal;
  font-size: 24px;
`;

H2Wrapper.displayName = 'H2';

const H2 = ({ children, ...rest }) => {
  return <H2Wrapper {...rest}>{children}</H2Wrapper>;
};

H2.propTypes = {
  children: PropTypes.any,
};

export default H2;
