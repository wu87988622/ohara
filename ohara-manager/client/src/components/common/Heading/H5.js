import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const Wrapper = styled.h5`
  font-size: 18px;
`;

const H5 = ({ children, ...rest }) => {
  return <Wrapper {...rest}>{children}</Wrapper>;
};

H5.propTypes = {
  children: PropTypes.any,
};

export default H5;
