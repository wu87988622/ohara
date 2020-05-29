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
import TextField from '@material-ui/core/TextField';
import styled from 'styled-components';

const InputWrap = styled.div`
  position: relative;
  width: 100%;
`;

const StyledTextField = styled(TextField)`
  width: ${props => props.width};
`;

const InputField = props => {
  const {
    input: { name, onChange, value, ...restInput },
    meta = {},
    width = '100%',
    helperText,
    ...rest
  } = props;

  const hasError = meta.error && meta.touched;

  return (
    <InputWrap>
      <StyledTextField
        {...rest}
        error={hasError}
        helperText={hasError ? meta.error : helperText}
        InputProps={restInput}
        name={name}
        onChange={onChange}
        value={value}
        width={width}
      />
    </InputWrap>
  );
};

InputField.propTypes = {
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
};

export default InputField;
