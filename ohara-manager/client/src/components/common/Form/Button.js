import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  white,
  blue,
  blueHover,
  radiusNormal,
  durationNormal,
} from '../../../theme/variables';

const ButtonWrapper = styled.button`
  font-size: 13px;
  font-family: inherit;
  border: 0;
  border-radius: ${radiusNormal};
  color: ${white};
  padding: 12px 16px;
  background-color: ${blue};
  width: ${({ width }) => width};
  transition: ${durationNormal} all;

  &:hover {
    background-color: ${blueHover};
    transition: ${durationNormal} all;
  }
`;

const IconWrapper = styled.i`
  margin-left: 8px;
`;

const Button = ({ type, text, width = 'auto', ...rest }) => {
  return (
    <ButtonWrapper type={type} width={width} {...rest}>
      {text} <IconWrapper className="fas fa-long-arrow-alt-right" />
    </ButtonWrapper>
  );
};

Button.propTypes = {
  type: PropTypes.string.isRequired,
  text: PropTypes.string.isRequired,
  width: PropTypes.string.isRequired,
};

export default Button;
