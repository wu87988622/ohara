import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import cx from 'classnames';

import { defaultBtn } from '../../../theme/btnTheme';
import {
  radiusNormal,
  durationNormal,
  lighterGray,
  lightGray,
  white,
  blue,
} from '../../../theme/variables';

const BtnWrapper = styled.button`
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
    border: ${props => props.theme.borderHover};
    background-color: ${props => props.theme.bgHover};
    color: ${props => props.theme.colorHover};
    transition: ${durationNormal} all;
  }

  &:focus {
    border-color: ${blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${durationNormal} all;
  }

  &.is-working,
  &.is-disabled {
    border: 1px solid ${lighterGray};
    cursor: not-allowed;
    color: ${lightGray};
    background-color: ${white};
  }
`;

BtnWrapper.displayName = 'BtnWrapper';

const IconWrapper = styled.i`
  margin-left: 5px;
`;

const Button = ({
  text,
  handleClick = () => {},
  theme = defaultBtn,
  type = 'submit',
  width = 'auto',
  disabled = false,
  isWorking = false,
  ...rest
}) => {
  const cls = cx({ 'is-working': isWorking }, { 'is-disabled': disabled });

  return (
    <BtnWrapper
      type={type}
      width={width}
      onClick={handleClick}
      theme={theme}
      className={cls}
      disabled={disabled}
      {...rest}
    >
      {text} {isWorking && <IconWrapper className="fas fa-spinner fa-spin" />}
    </BtnWrapper>
  );
};

Button.propTypes = {
  text: PropTypes.string.isRequired,
  handleClick: PropTypes.func,
  theme: PropTypes.shape({
    color: PropTypes.string,
    bgColor: PropTypes.string,
    border: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    bgHover: PropTypes.string,
    colorHover: PropTypes.string,
    borderHover: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
  type: PropTypes.string,
  width: PropTypes.string,
  disabled: PropTypes.bool,
  isWorking: PropTypes.bool,
};

export default Button;
