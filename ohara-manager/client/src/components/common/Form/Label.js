import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { lightBlue } from 'theme/variables';

const LabelWrapper = styled.label`
  color: ${lightBlue};
  width: ${({ width }) => width};
  font-size: 13px;
  margin: ${({ margin }) => margin};
`;

LabelWrapper.displayName = 'Label';

const Label = ({
  children,
  css = { margin: '0 0 8px', width: 'auto' },
  ...rest
}) => {
  return (
    <LabelWrapper {...css} {...rest}>
      {children}
    </LabelWrapper>
  );
};

Label.propTypes = {
  children: PropTypes.any,
  css: PropTypes.object,
};

export default Label;
