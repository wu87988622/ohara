import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const H5Wrapper = styled.h5`
  font-size: 18px;
`;

H5Wrapper.displayName = 'H5Wrapper';

const H5 = ({ children, ...rest }) => {
  return <H5Wrapper {...rest}>{children}</H5Wrapper>;
};

H5.propTypes = {
  children: PropTypes.any,
};

export default H5;
