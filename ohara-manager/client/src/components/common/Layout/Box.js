import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { white, shadowNormal, radiusNormal } from '../../../theme/variables';

const BoxWrapper = styled.div`
  padding: 25px;
  background-color: ${white};
  box-shadow: ${shadowNormal};
  border-radius: ${radiusNormal};
  margin-bottom: 20px;
`;

BoxWrapper.displayName = 'BoxWrapper';

const Box = ({ children, ...rest }) => {
  return <BoxWrapper {...rest}>{children}</BoxWrapper>;
};

Box.propTypes = {
  children: PropTypes.any,
};

export default Box;
