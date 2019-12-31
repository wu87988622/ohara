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
import TextField from '@material-ui/core/TextField';

const ObjectKey = props => {
  const {
    input: { name, onChange, value, ...restInput },
    meta = {},
    helperText,
    label,
    refs,
    ...rest
  } = omit(props, ['tableKeys']);

  const hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);

  return (
    <>
      <TextField
        {...rest}
        ref={refs}
        onChange={onChange}
        name={name + 'Name'}
        InputProps={restInput}
        value={value}
        fullWidth
        label={label + ' (name)'}
        helperText={hasError ? meta.error : helperText}
        error={hasError}
      />
      <TextField
        {...rest}
        onChange={onChange}
        name={name + 'Group'}
        InputProps={restInput}
        value={value}
        fullWidth
        label={label + ' (group)'}
        helperText={hasError ? meta.error : helperText}
        error={hasError}
      />
    </>
  );
};

ObjectKey.propTypes = {
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
  label: PropTypes.string,
  refs: PropTypes.object,
};

export default ObjectKey;
