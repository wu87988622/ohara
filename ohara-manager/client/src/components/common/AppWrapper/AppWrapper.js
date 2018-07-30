import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const Wrapper = styled.div`
  margin-top: 68px;
`;

const H2 = styled.h2`
  border-bottom: 1px solid #e5e5e5;
  padding-bottom: 8px;
`;

const AppWrapper = ({ title, children }) => {
  return (
    <Wrapper className="col-md-9 ml-sm-auto col-lg-10 px-4">
      <H2>{title}</H2>
      {children}
    </Wrapper>
  );
};

AppWrapper.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.any,
};

export default AppWrapper;
