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
import { map, isString } from 'lodash';
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
    testId,
  } = props;

  const getDisplayNameByValue = item => {
    if (props?.getDisplayNameByValue) {
      return props.getDisplayNameByValue(item.value);
    }
    // if neither dispalyName nor func defined, we use value as displayName by default
    return item?.displayName ? item.displayName : item.value;
  };

  return (
    <Typography component="div" data-testid={testId}>
      <FormControl disabled={disabled}>
        <MuiSelect
          input={<StyledInputBase />}
          MenuProps={{
            getContentAnchorEl: null,
            anchorEl,
            anchorOrigin,
            transformOrigin,
          }}
          onChange={onChange}
          onOpen={event => setAnchorEl(event.currentTarget)}
          value={value}
        >
          {map(list, item => {
            if (isString(item)) {
              return (
                <MenuItem key={item} value={item}>
                  {item}
                </MenuItem>
              );
            } else {
              return (
                <MenuItem key={item.value} value={item.value}>
                  {getDisplayNameByValue(item)}
                </MenuItem>
              );
            }
          })}
        </MuiSelect>
      </FormControl>
    </Typography>
  );
};

Select.propTypes = {
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  list: PropTypes.arrayOf(
    PropTypes.oneOfType([
      PropTypes.shape({
        value: PropTypes.string.isRequired,
        displayName: PropTypes.string,
      }),
      PropTypes.string,
    ]),
  ).isRequired,
  disabled: PropTypes.bool,
  anchorOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
  transformOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
  getDisplayNameByValue: PropTypes.func,
  testId: PropTypes.string,
};

Select.defaultProps = {
  disabled: false,
  anchorOrigin: {
    vertical: 'bottom',
    horizontal: 'center',
  },
  transformOrigin: {
    vertical: 'top',
    horizontal: 'center',
  },
  getDisplayNameByValue: null,
  testId: 'select-component',
};

export default Select;
