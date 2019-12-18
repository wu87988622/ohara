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
import Chip from '@material-ui/core/Chip';
import styled, { css } from 'styled-components';
import TextField from '@material-ui/core/TextField';
import Autocomplete from '@material-ui/lab/Autocomplete';

const SttledAutocomplete = styled(Autocomplete)(
  () => css`
    .MuiFilledInput-root {
      background-color: Transparent;
    }
  `,
);

const ArrayDef = props => {
  const {
    input: { name, onChange, value: formValue = [] },
    meta = {},
    helperText,
    refs,
    ...rest
  } = omit(props, ['tableKeys']);

  const hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);

  return (
    <SttledAutocomplete
      ref={refs}
      multiple
      id="tags-filled"
      freeSolo
      renderTags={(value, getTagProps) => {
        onChange(value);
        return [...formValue, value].map((option, index) => (
          <Chip variant="outlined" label={option} {...getTagProps({ index })} />
        ));
      }}
      renderInput={params => {
        return (
          <TextField
            {...params}
            {...rest}
            placeholder={'Please press enter to add'}
            fullWidth
            variant="filled"
            name={name}
            helperText={hasError ? meta.error : helperText}
            error={hasError}
            type="text"
          />
        );
      }}
    />
  );
};

ArrayDef.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.object,
      PropTypes.array,
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

export default ArrayDef;
