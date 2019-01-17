/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import {
  blue,
  lightGray,
  lighterGray,
  radiusNormal,
  durationNormal,
  lightBlue,
} from 'theme/variables';

const InputWrapper = styled.input`
  font-size: 12px;
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

  &.is-disabled {
    border: 1px solid ${lighterGray};
    cursor: not-allowed;
    color: ${lightGray};
  }
`;

InputWrapper.displayName = 'Input';

const Input = ({
  type = 'text',
  value,
  handleChange,
  placeholder = '',
  width = '120px',
  height = '32px',
  disabled = false,
  ...rest
}) => {
  const disableCls = disabled ? 'is-disabled' : '';

  return (
    <InputWrapper
      type={type}
      value={value}
      onChange={handleChange}
      placeholder={placeholder}
      width={width}
      height={height}
      disabled={disabled}
      className={disableCls}
      {...rest}
    />
  );
};

Input.propTypes = {
  type: PropTypes.string,
  value: PropTypes.string.isRequired,
  handleChange: PropTypes.func,
  placeholder: PropTypes.string,
  width: PropTypes.string,
  height: PropTypes.string,
  disabled: PropTypes.bool,
};

export default Input;
