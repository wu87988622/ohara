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
import TextField from '@material-ui/core/TextField';
import MenuItem from '@material-ui/core/MenuItem';

// Mui TextField also supports native select, that's why
// we're not using Mui's <Select /> component directly here
const StyledSelect = styled(TextField)`
  width: ${props => props.width};
  margin: ${props => props.theme.spacing(1)}px;
`;

const Select = props => {
  const {
    input: { name, onChange, value, ...restInput },
    meta = {},
    list = [],
    width = '100%',
    disables = [],
    ...rest
  } = props;

  const placeholder = 'Please select...';
  const _list = [placeholder, ...list];
  const _value = value ? value : placeholder;
  const error = meta.error && meta.touched;

  return (
    <StyledSelect
      {...rest}
      error={error}
      helperText={error && meta.error}
      InputProps={restInput}
      name={name}
      onChange={onChange}
      select={true}
      value={_value}
      width={width}
    >
      {_list.map(item => {
        const disabled = disables.includes(item);
        return (
          <MenuItem disabled={disabled} key={item} value={item}>
            {item}
          </MenuItem>
        );
      })}
    </StyledSelect>
  );
};

Select.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string.isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    touched: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  width: PropTypes.string,
  list: PropTypes.array.isRequired,
  disables: PropTypes.arrayOf(PropTypes.string),
};

export default Select;
