import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { lightBlue } from 'theme/variables';

const LabelWrapper = styled.label`
  color: ${lightBlue};
  font-size: 13px;
  margin-bottom: 8px;
`;

LabelWrapper.displayName = 'Label';

LabelWrapper.displayName = 'Label';

const Label = ({ children, ...rest }) => {
  return <LabelWrapper {...rest}>{children}</LabelWrapper>;
};

Label.propTypes = {
  children: PropTypes.any,
};

export default Label;
