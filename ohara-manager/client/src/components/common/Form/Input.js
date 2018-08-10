import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  blue,
  lighterGray,
  radiusNormal,
  durationNormal,
  lightBlue,
} from '../../../theme/variables';

const InputWrapper = styled.input`
  font-size: 13px;
  font-family: inherit;
  color: ${lightBlue};
  border: 1px solid ${lighterGray};
  padding: 10px 10px 10px 15px;
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  border-radius: ${radiusNormal};
  outline: none;
  transition: ${durationNormal} all;

  &:focus {
    border-color: ${blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${durationNormal} all;
  }
`;

InputWrapper.displayName = 'Input';

const Input = ({
  type = 'text',
  value,
  handleChange,
  placeholder = '',
  width = '120px',
  height = '40px',
  ...rest
}) => {
  return (
    <InputWrapper
      type={type}
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      width={width}
      height={height}
      {...rest}
    />
  );
};

Input.propTypes = {
  value: PropTypes.string.isRequired,
  handleChange: PropTypes.func.isRequired,
  type: PropTypes.string,
  width: PropTypes.string,
  height: PropTypes.string,
};

export default Input;
