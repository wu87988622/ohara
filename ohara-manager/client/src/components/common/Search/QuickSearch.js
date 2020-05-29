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

import React, { useEffect, useState, useCallback } from 'react';
import PropTypes from 'prop-types';
import { debounce, noop, trim } from 'lodash';
import styled, { css } from 'styled-components';

import Paper from '@material-ui/core/Paper';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import SearchIcon from '@material-ui/icons/Search';
import { usePrevious } from 'utils/hooks';
import { createSearch } from './search';

export const Wrapper = styled.div(
  ({ theme }) => css`
    .MuiPaper-root {
      padding: '${theme.spacing(0.5, 1)}px';
      display: 'flex';
      align-items: 'center';
    }
    .MuiInputBase-input {
      margin-right: ${theme.spacing(3)}px;
    }

    .MuiIconButton-root {
      padding: ${props =>
        props.size === 'sm' ? theme.spacing(0.75, 0.75) : theme.spacing(1.5)};
    }
  `,
);

const QuickSearch = ({
  data,
  isDebounce,
  keys,
  placeholder,
  setResults,
  size,
  wait,
}) => {
  const prevData = usePrevious(data);
  const [searchText, setSearchText] = useState('');
  const prevSearchText = usePrevious(searchText);

  const search = useCallback(createSearch(keys), [keys]);

  useEffect(() => {
    if (data === prevData && searchText === prevSearchText) return;
    if (!searchText) setResults(data);
    else setResults(search(data, searchText));
  }, [data, prevData, searchText, prevSearchText, search, setResults]);

  const handleChange = debounce(
    value => setSearchText(trim(value)),
    isDebounce ? wait : 0,
  );

  return (
    <Wrapper size={size}>
      <Paper component="form">
        <IconButton aria-label="search">
          <SearchIcon />
        </IconButton>
        <InputBase
          inputProps={{ 'aria-label': 'Search' }}
          onChange={event => handleChange(event.target.value)}
          placeholder={placeholder}
          type="search"
        />
      </Paper>
    </Wrapper>
  );
};

QuickSearch.propTypes = {
  data: PropTypes.array,
  keys: PropTypes.array,
  placeholder: PropTypes.string,
  isDebounce: PropTypes.bool,
  wait: PropTypes.number, // the number of milliseconds to delay
  setResults: PropTypes.func,
  size: PropTypes.oneOf(['sm', 'lg']),
};

QuickSearch.defaultProps = {
  data: [],
  isDebounce: true,
  keys: ['name'],
  placeholder: 'Search',
  setResults: noop,
  size: 'lg',
  wait: 500,
};

export default QuickSearch;
