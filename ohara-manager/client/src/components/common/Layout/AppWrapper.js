import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { H2 } from 'common/Headings';
import { white, radiusNormal, shadowNormal } from '../../../theme/variables';

const Wrapper = styled.div`
  padding: 100px 50px;
`;

Wrapper.displayName = 'Wrapper';

const Main = styled.div`
  background-color: ${white};
  border-radius: ${radiusNormal};
  box-shadow: ${shadowNormal};
`;

Main.displayName = 'Main';

const AppWrapper = ({ title, children }) => {
  return (
    <Wrapper>
      <H2>{title}</H2>
      <Main>{children}</Main>
    </Wrapper>
  );
};

AppWrapper.propTypes = {
  title: PropTypes.string.isRequired,
  children: PropTypes.any,
};

export default AppWrapper;
