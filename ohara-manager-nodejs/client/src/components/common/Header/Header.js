import React from 'react';
import PropTypes from 'prop-types';
import styled, { keyframes } from 'styled-components';

import logo from '../../../logo.svg';

const Wrapper = styled.header`
  background-color: #222;
  height: 12rem;
  padding: 1rem;
  color: white;
  margin-bottom: 20px;
`;

const rotate360 = keyframes`
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
`;

const Logo = styled.img`
  animation: ${rotate360} infinite 10s linear;
  height: 80px;
`;

const Title = styled.h1`
  font-size: 1.5em;
`;

const Header = ({ props }) => {
  return (
    <Wrapper className="App-header">
      <Logo src={logo} alt="logo" />
      <Title>Welcome to React</Title>
    </Wrapper>
  );
};

Header.propTypes = {
  prop: PropTypes.array,
};

export default Header;
