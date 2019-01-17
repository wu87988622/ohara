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
  white,
  blue,
  lightBlue,
  lighterGray,
  radiusNormal,
  durationNormal,
} from 'theme/variables';

const SelectWrapper = styled.select`
  font-size: 13px;
  font-family: inherit;
  color: ${lightBlue};
  border: 1px solid ${lighterGray};
  padding: 10px 10px 10px 15px;
  border-radius: ${radiusNormal};
  background-color: ${white};
  width: ${({ width }) => width};
  height: ${({ height }) => height};
  outline: 0;

  &:focus {
    border-color: ${blue};
    box-shadow: 0 0 0 3px rgba(76, 132, 255, 0.25);
    transition: ${durationNormal} all;
  }
`;

SelectWrapper.displayName = 'Select';

const Select = ({
  list = [],
  selected = '',
  handleChange,
  isObject = false,
  height = '32px',
  width = '100%',
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
      {isObject
        ? list.map(({ id, name }, idx) => {
            return (
              <option key={idx} data-id={id}>
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
  isObject: PropTypes.bool,
  width: PropTypes.string,
  height: PropTypes.string,
  list: PropTypes.array,
};

export default Select;
