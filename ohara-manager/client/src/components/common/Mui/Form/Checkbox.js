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
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import styled from 'styled-components';

import Tooltip from './Tooltip';

const InputWrap = styled.div`
  position: relative;
  width: 100%;
`;

const TooltipWrap = styled.div`
  position: absolute;
  top: 0;
  right: 0;
`;

const MuiCheckbox = ({
  input: { name, onChange, checked, ...restInput },
  meta,
  label,
  helperText,
  ...rest
}) => {
  return (
    <InputWrap>
      <FormControlLabel
        control={
          <Checkbox
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

      <TooltipWrap>
        <Tooltip text={helperText} />
      </TooltipWrap>
    </InputWrap>
  );
};

MuiCheckbox.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
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
};

export default MuiCheckbox;
