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
import { size } from 'lodash';
import Checkbox from '@material-ui/core/Checkbox';
import TextField from '@material-ui/core/TextField';
import Autocomplete from '@material-ui/lab/Autocomplete';
import CheckBoxOutlineBlankIcon from '@material-ui/icons/CheckBoxOutlineBlank';
import CheckBoxIcon from '@material-ui/icons/CheckBox';
import styled from 'styled-components';

const icon = <CheckBoxOutlineBlankIcon fontSize="small" />;
const checkedIcon = <CheckBoxIcon fontSize="small" />;

const Wrapper = styled.div`
  position: relative;
  width: 100%;
`;

const ArrayField = (props) => {
  const {
    input: { name, onChange, value },
    meta: { error, touched, dirty },
    disabled,
    helperText,
    label,
    multipleChoice,
    options,
    required,
  } = props;
  const getOptionLabel = (option) => {
    if (props?.getOptionLabel) {
      return props.getOptionLabel(option);
    }
    return typeof option === 'string' ? option : JSON.stringify(option);
  };

  const hasError = (touched || dirty) && error;

  const placeholder = !multipleChoice
    ? 'Please select item from the dropdown'
    : size(value) === 0
    ? 'Please select items from the dropdown'
    : 'Select more';

  return (
    <Wrapper>
      <Autocomplete
        disableClearable={multipleChoice}
        disableCloseOnSelect={multipleChoice}
        disabled={disabled}
        getOptionLabel={getOptionLabel}
        multiple={multipleChoice}
        onChange={(event, values) => {
          onChange(values);
        }}
        options={options}
        renderInput={(params) => (
          <TextField
            {...params}
            error={hasError}
            fullWidth
            helperText={hasError ? error : helperText}
            label={label}
            name={name}
            placeholder={placeholder}
            required={required}
          />
        )}
        renderOption={(option, { selected }) => (
          <React.Fragment>
            {multipleChoice && (
              <Checkbox
                checked={selected}
                checkedIcon={checkedIcon}
                color="primary"
                icon={icon}
              />
            )}
            {getOptionLabel(option)}
          </React.Fragment>
        )}
        value={value}
      />
    </Wrapper>
  );
};

ArrayField.propTypes = {
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
  disabled: PropTypes.bool,
  errorMessage: PropTypes.string,
  getOptionLabel: PropTypes.func,
  helperText: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
  label: PropTypes.string,
  multipleChoice: PropTypes.bool,
  options: PropTypes.arrayOf(
    PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.object,
      PropTypes.array,
    ]),
  ).isRequired,
  refs: PropTypes.object,
  required: PropTypes.bool,
};

ArrayField.defaultProps = {
  meta: {},
  disabled: false,
  errorMessage: null,
  getOptionLabel: null,
  helperText: null,
  label: 'Chooser',
  multipleChoice: false,
  refs: null,
  required: false,
};

export default ArrayField;
