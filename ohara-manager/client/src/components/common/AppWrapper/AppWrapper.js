import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { white, radiusNormal, shadowNormal } from '../../../theme/variables';

const Wrapper = styled.div`
  padding: 100px 30px 0 240px;
`;

const Main = styled.div`
  background-color: ${white};
  border-radius: ${radiusNormal};
  box-shadow: ${shadowNormal};
`;

const AppWrapper = ({ title, children }) => {
  return (
    <Wrapper>
      <h2>{title}</h2>
      <Main>{children}</Main>
    </Wrapper>
  );
};

AppWrapper.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.any,
};

export default AppWrapper;
