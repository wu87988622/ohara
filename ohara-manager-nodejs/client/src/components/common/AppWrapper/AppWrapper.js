import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const Wrapper = styled.div`
  text-align: center;
`;

const AppWrapper = ({ children }) => {
  return <Wrapper>{children}</Wrapper>;
};

AppWrapper.propTypes = {
  children: PropTypes.array,
};

export default AppWrapper;
