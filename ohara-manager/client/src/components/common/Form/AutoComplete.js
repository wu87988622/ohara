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
import { forEach, isEmpty } from 'lodash';
import TextField from '@material-ui/core/TextField';
import MuiAutoComplete from '@material-ui/lab/Autocomplete';
import styled from 'styled-components';

const Wrapper = styled.div`
  position: relative;
  width: 100%;
`;

const AutoComplete = (props) => {
  const {
    input: { name, onChange, value, ...restInput },
    meta,
    options,
    label,
    required,
    multiple,
    textFieldProps,
    getOptionValue,
    initializeValue,
    ...rest
  } = props;

  const { helperText, ...lessRest } = rest;
  const { variant, ...restTextFieldProps } = textFieldProps;

  const getValue = (values) => {
    if (!getOptionValue) {
      return values;
    }
    // ternary hell...
    return multiple
      ? values
        ? values.map(getOptionValue)
        : undefined
      : values
      ? getOptionValue(values)
      : undefined;
  };

  const showError =
    ((meta.submitError && !meta.dirtySinceLastSubmit) || meta.error) &&
    meta.touched;

  const handleChange = (_, values) => onChange(getValue(values));

  let defaultValue;

  if (!getOptionValue) {
    defaultValue = value;
  } else if (value !== undefined && value !== null) {
    forEach(options, (option) => {
      const optionValue = getOptionValue(option);
      if (multiple) {
        if (!defaultValue) defaultValue = [];
        forEach(value, (v) => {
          if (v === optionValue) defaultValue.push(option);
        });
      } else {
        if (value === optionValue) {
          defaultValue = option;
        }
      }
    });
  }
  const newValue = isEmpty(defaultValue) ? initializeValue : value;
  return (
    <Wrapper>
      <MuiAutoComplete
        multiple={multiple}
        onChange={handleChange}
        options={options}
        renderInput={(params) => (
          <TextField
            error={showError}
            fullWidth={true}
            helperText={showError ? meta.error || meta.submitError : helperText}
            label={label}
            margin="normal"
            name={name}
            required={required}
            variant={variant}
            {...params}
            {...restInput}
            {...restTextFieldProps}
          />
        )}
        value={newValue}
        {...lessRest}
      />
    </Wrapper>
  );
};

AutoComplete.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.object,
    ]).isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    dirty: PropTypes.bool,
    dirtySinceLastSubmit: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    touched: PropTypes.bool,
    submitError: PropTypes.any,
  }).isRequired,
  label: PropTypes.string.isRequired,
  required: PropTypes.bool,
  multiple: PropTypes.bool,
  getOptionValue: PropTypes.func,
  options: PropTypes.array.isRequired,
  textFieldProps: PropTypes.object,
  initializeValue: PropTypes.array,
};

AutoComplete.defaultProps = {
  required: false,
  multiple: false,
  getOptionValue: null,
  textFieldProps: {
    variant: 'standard',
  },
};

export default AutoComplete;
