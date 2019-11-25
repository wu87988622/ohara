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
  `,
);

const QuickSearch = ({
  data = [],
  keys = ['name'],
  placeholder = 'Search',
  isDebounce = true,
  wait = 500,
  setResults = noop,
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
    <Wrapper>
      <Paper component="form">
        <IconButton aria-label="search">
          <SearchIcon />
        </IconButton>
        <InputBase
          type="search"
          placeholder={placeholder}
          inputProps={{ 'aria-label': 'Search' }}
          onChange={event => handleChange(event.target.value)}
        />
      </Paper>
    </Wrapper>
  );
};

QuickSearch.propTypes = {
  data: PropTypes.array.isRequired,
  keys: PropTypes.array, // default ['name']
  placeholder: PropTypes.string, // default 'Search'
  isDebounce: PropTypes.bool, // default true
  wait: PropTypes.number, // default 500, the number of milliseconds to delay
  setResults: PropTypes.func, // noop
};

export default QuickSearch;
