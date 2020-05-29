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

  const [objectKey, setObjectKey] = React.useState({
    name: value.name ? value.name : '',
    group: value.group ? value.group : '',
  });

  const handleChange = event => {
    if (event.target.name.endsWith('Name')) {
      setObjectKey({ ...objectKey, name: event.target.value });
    } else {
      setObjectKey({ ...objectKey, group: event.target.value });
    }
    onChange(objectKey);
  };

  return (
    <>
      <TextField
        {...rest}
        error={hasError}
        fullWidth
        helperText={hasError ? meta.error : helperText}
        InputProps={restInput}
        label={label + ' (name)'}
        name={name + 'Name'}
        onChange={handleChange}
        ref={refs}
        value={objectKey.name}
      />
      <TextField
        {...rest}
        error={hasError}
        fullWidth
        helperText={hasError ? meta.error : helperText}
        InputProps={restInput}
        label={label + ' (group)'}
        name={name + 'Group'}
        onChange={handleChange}
        value={objectKey.group}
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
