import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const Wrapper = styled.div`
  margin-top: 100px;
  margin-left: 240px;
`;

const AppWrapper = ({ title, children }) => {
  return (
    <Wrapper>
      <h2>{title}</h2>
      {children}
    </Wrapper>
  );
};

AppWrapper.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.any,
};

export default AppWrapper;
