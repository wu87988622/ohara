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
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import FormHelperText from '@material-ui/core/FormHelperText';

const BooleanDef = props => {
  const {
    input,
    meta = {},
    label,
    testId,
    helperText,
    refs,
    ...rest
  } = omit(props, ['tableKeys']);

  const { name, onChange, checked = false, ...restInput } = omit(input, [
    'type',
  ]);

  const hasError = (meta.error && meta.touched) || (meta.error && meta.dirty);
  return (
    <div ref={refs}>
      <FormControlLabel
        control={
          <Checkbox
            data-testid={testId}
            {...rest}
            checked={checked}
            color="primary"
            inputProps={{ type: 'checkbox', ...restInput }}
            name={name}
            onChange={onChange}
          />
        }
        label={label}
      />
      <FormHelperText children={hasError ? meta.error : helperText} />
    </div>
  );
};

BooleanDef.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    checked: PropTypes.bool,
    value: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.bool,
    ]).isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    touched: PropTypes.bool,
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  }).isRequired,
  label: PropTypes.string,
  helperText: PropTypes.string,
  testId: PropTypes.string,
  refs: PropTypes.object,
};
BooleanDef.defaultProps = {
  helperText: '',
};

export default BooleanDef;
