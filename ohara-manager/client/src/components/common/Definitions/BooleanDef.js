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

const BooleanDef = props => {
  const {
    input: { name, onChange, checked = false, ...restInput },
    label,
    testId,
    refs,
    ...rest
  } = omit(props, ['tableKeys', 'helperText']);

  return (
    <FormControlLabel
      ref={refs}
      control={
        <Checkbox
          data-testid={testId}
          {...rest}
          onChange={onChange}
          name={name}
          inputProps={restInput}
          checked={checked}
          color="primary"
        />
      }
      label={label}
    />
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

export default BooleanDef;