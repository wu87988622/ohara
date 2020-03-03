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
import { omit, isObject } from 'lodash';
import PropTypes from 'prop-types';
import TextField from '@material-ui/core/TextField';

const Tags = props => {
  const {
    input: { name, onChange, value, ...restInput },
    meta = {},
    helperText,
    refs,
    ...rest
  } = omit(props, ['tableKeys']);

  let hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);
  let jsonValue = value;
  try {
    jsonValue = JSON.stringify(JSON.parse(value), 0, 2);
  } catch (error) {
    // to covert the initial value (from backend) which should be JSON object
    if (isObject(jsonValue)) {
      jsonValue = JSON.stringify(jsonValue, 0, 2);
    } else {
      hasError = true;
      meta.error = 'Invalid JSON format';
    }
  }

  return (
    <TextField
      {...rest}
      InputProps={restInput}
      ref={refs}
      fullWidth
      variant="outlined"
      onChange={onChange}
      name={name}
      value={jsonValue}
      helperText={hasError ? meta.error : helperText}
      error={hasError}
      multiline
      rows="10"
      type="text"
    />
  );
};

Tags.propTypes = {
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

export default Tags;
