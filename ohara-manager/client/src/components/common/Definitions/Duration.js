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
import { omit } from 'lodash';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';
import TextField from '@material-ui/core/TextField';
import InputAdornment from '@material-ui/core/InputAdornment';

const convertUnit = value => {
  const hasSeparator = value.length > 0 && value.includes(' ');

  if (hasSeparator) {
    // conversion: since backend will always give us millisecond, so we're doing
    // milliseconds -> seconds (which is divided by 1000) here
    const newValue = value.split(' ').shift();
    return newValue / 1000;
  }

  return value;
};

const StyledTextField = styled(TextField)(
  () => css`
    .MuiFilledInput-root {
      background-color: white;
    }
  `,
);

const Duration = props => {
  const { input, meta = {}, helperText, refs, ...rest } = omit(props, [
    'tableKeys',
  ]);

  const { name, onChange, value, ...restInput } = omit(input, ['type']);

  const hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);

  const handleChange = event => {
    // Does the conversion again here
    const unit = 'milliseconds';
    const value = `${event.target.value * 1000} ${unit}`;

    // The converted value will be used when it's submitted
    onChange(value);
  };

  return (
    <StyledTextField
      {...rest}
      error={hasError}
      fullWidth
      helperText={hasError ? meta.error : helperText}
      InputProps={{
        ...restInput,
        endAdornment: <InputAdornment position="end">Seconds</InputAdornment>,
        // Props for native input element
        inputProps: { min: 0 },
      }}
      name={name}
      onChange={handleChange}
      ref={refs}
      type="number"
      value={convertUnit(value)}
    />
  );
};

Duration.propTypes = {
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
    touched: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }),
  width: PropTypes.string,
  helperText: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  errorMessage: PropTypes.string,
  refs: PropTypes.object,
};

export default Duration;
