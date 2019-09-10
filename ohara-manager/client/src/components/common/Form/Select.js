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

const SelectWrapper = styled.select`
  font-size: 13px;
  font-family: inherit;
  color: ${({ disabled, theme }) =>
    disabled ? theme.lightGray : theme.lightBlue};
  border: 1px solid ${props => props.theme.lighterGray};
  border-radius: ${props => props.theme.radiusNormal};
  background-color: ${props => props.theme.white};
  width: ${({ width }) => (width ? width : '100%')};
  height: ${({ height }) => (height ? height : '36px')};
  outline: 0;

  &:focus {
    border-color: ${props => props.theme.blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${props => props.theme.durationNormal} all;
  }
`;

SelectWrapper.displayName = 'Select';

const Select = ({
  list = [],
  selected = '',
  handleChange,
  placeholder,
  height,
  width,
  isObject = false,
  clearable = false,
  ...rest
}) => {
  const _selected = isObject ? selected && selected.name : selected;

  return (
    <SelectWrapper
      value={_selected}
      onChange={handleChange}
      width={width}
      height={height}
      {...rest}
    >
      {(placeholder || clearable) && (
        <option value="" disabled={!clearable} data-id="default-option">
          {placeholder || 'Please select...'}
        </option>
      )}
      {isObject
        ? list.map(({ id, name, disabled }, idx) => {
            return (
              <option value={name} key={idx} data-id={id} disabled={disabled}>
                {name}
              </option>
            );
          })
        : list.map((name, idx) => {
            return <option key={idx}>{name}</option>;
          })}
    </SelectWrapper>
  );
};

Select.propTypes = {
  selected: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  handleChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  isObject: PropTypes.bool,
  width: PropTypes.string,
  height: PropTypes.string,
  list: PropTypes.array,
  clearable: PropTypes.bool,
};

export default Select;
