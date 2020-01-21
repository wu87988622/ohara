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
import Typography from '@material-ui/core/Typography';
import FormControl from '@material-ui/core/FormControl';
import MuiSelect from '@material-ui/core/Select';
import MenuItem from '@material-ui/core/MenuItem';
import InputBase from '@material-ui/core/InputBase';
import styled, { css } from 'styled-components';

const StyledInputBase = styled(InputBase)(
  ({ theme }) => css`
    width: 160px;
    border: 1px solid ${theme.palette.grey[200]};
    border-radius: ${theme.shape.borderRadius}px;
    margin: ${theme.spacing(1)}px;

    .MuiSelect-selectMenu {
      padding-left: ${theme.spacing(1)}px;
    }
  `,
);

const Select = props => {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const {
    value,
    onChange,
    list,
    disabled,
    anchorOrigin,
    transformOrigin,
  } = props;

  return (
    <Typography component="div">
      <FormControl disabled={disabled}>
        <MuiSelect
          value={value}
          onOpen={event => setAnchorEl(event.currentTarget)}
          onChange={onChange}
          input={<StyledInputBase />}
          MenuProps={{
            getContentAnchorEl: null,
            anchorEl,
            anchorOrigin,
            transformOrigin,
          }}
        >
          {list.map(item => {
            return (
              <MenuItem value={item} key={item}>
                {item}
              </MenuItem>
            );
          })}
        </MuiSelect>
      </FormControl>
    </Typography>
  );
};

Select.propTypes = {
  value: PropTypes.string.isRequired,
  list: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  anchorOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
  transformOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
};

Select.defaultProps = {
  anchorOrigin: {
    vertical: 'bottom',
    horizontal: 'center',
  },
  transformOrigin: {
    vertical: 'top',
    horizontal: 'center',
  },
  disabled: false,
};

export default Select;
