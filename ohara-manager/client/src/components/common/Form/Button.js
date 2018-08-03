import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { defaultButton } from '../../../theme/buttonTheme';
import { radiusNormal, durationNormal } from '../../../theme/variables';

const ButtonWrapper = styled.button`
  font-size: 13px;
  font-family: inherit;
  border: ${props => props.theme.border};
  border-radius: ${radiusNormal};
  color: ${props => props.theme.color};
  padding: 12px 16px;
  background-color: ${props => props.theme.bgColor};
  width: ${({ width }) => width};
  transition: ${durationNormal} all;

  &:hover {
    border: ${props => props.theme.borderHover}
    background-color: ${props => props.theme.bgHover};
    color: ${props => props.theme.colorHover};
    transition: ${durationNormal} all;
  }
`;

const Button = ({
  text,
  handleClick,
  theme = defaultButton,
  type = 'submit',
  width = 'auto',
  ...rest
}) => {
  return (
    <ButtonWrapper
      type={type}
      width={width}
      onClick={handleClick}
      theme={theme}
      {...rest}
    >
      {text}
    </ButtonWrapper>
  );
};

Button.propTypes = {
  text: PropTypes.string.isRequired,
  theme: PropTypes.shape({
    color: PropTypes.string.isRequired,
    bgColor: PropTypes.string.isRequired,
    border: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
    bgHover: PropTypes.string.isRequired,
    colorHover: PropTypes.string.isRequired,
    borderHover: PropTypes.oneOfType([PropTypes.string, PropTypes.number])
      .isRequired,
  }),
  type: PropTypes.string,
  handleClick: PropTypes.func,
  width: PropTypes.string,
};

export default Button;
