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

const InputWrapper = styled.input`
  font-size: 12px;
  font-family: inherit;
  text-indent: 8px;
  outline: none;
  border: 1px solid ${props => props.theme.lighterGray};
  color: ${props => props.theme.lightBlue};
  width: ${({ width }) => (width ? width : '120px')};
  height: ${({ height }) => (height ? height : '36px')};
  border-radius: ${props => props.theme.radiusNormal};
  transition: ${props => props.theme.durationNormal} all;

  &:focus {
    border-color: ${props => props.theme.blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${props => props.theme.durationNormal} all;
  }

  &.is-disabled {
    border: 1px solid ${props => props.theme.lighterGray};
    background-color: ${props => props.theme.whiteSmoke};
    cursor: not-allowed;
    color: ${props => props.theme.lighterBlue};
  }
`;

InputWrapper.displayName = 'Input';

const Input = ({
  type = 'text',
  placeholder = '',
  disabled = false,
  handleChange,
  value,
  width,
  height,
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
