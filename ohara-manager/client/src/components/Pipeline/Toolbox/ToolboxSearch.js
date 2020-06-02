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

import React, { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import InputBase from '@material-ui/core/InputBase';
import SearchIcon from '@material-ui/icons/Search';
import IconButton from '@material-ui/core/IconButton';
import styled, { css } from 'styled-components';
import { isEmpty } from 'lodash';

import { KIND } from 'const';
import { usePrevious, useDebounce } from 'utils/hooks';

const StyledToolboxSearch = styled.div(
  (props) => css`
    display: flex;

    .MuiInputBase-root {
      width: 100%;
      padding-right: ${props.theme.spacing(3)}px;
    }
  `,
);

const ToolboxSearch = ({ searchData, setSearchResults, pipelineDispatch }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const debouncedSearchTerm = useDebounce(searchTerm, 500);
  const prevSearchTerm = usePrevious(debouncedSearchTerm);

  useEffect(() => {
    if (prevSearchTerm === debouncedSearchTerm) return;
    if (debouncedSearchTerm) {
      let sources = [];
      let sinks = [];
      let topics = [];
      let streams = [];

      searchData.forEach((data) => {
        const { name, kind } = data;
        const lowercaseName = name.toLowerCase();

        if (lowercaseName.includes(debouncedSearchTerm)) {
          if (kind === KIND.source) sources.push(data);
          if (kind === KIND.topic) topics.push(data);
          if (kind === KIND.stream) streams.push(data);
          if (kind === KIND.sink) sinks.push(data);
        }
      });

      // Open panels that contain results
      pipelineDispatch({
        type: 'setMultiplePanels',
        payload: {
          source: !isEmpty(sources),
          sink: !isEmpty(sinks),
          topic: !isEmpty(topics),
          stream: !isEmpty(streams),
        },
      });

      setSearchResults({ sources, sinks, topics, streams });
    } else {
      setSearchResults(null);
    }
  }, [
    debouncedSearchTerm,
    pipelineDispatch,
    prevSearchTerm,
    searchData,
    setSearchResults,
  ]);

  return (
    <StyledToolboxSearch>
      <IconButton>
        <SearchIcon />
      </IconButton>
      <InputBase
        onChange={(event) => setSearchTerm(event.target.value)}
        placeholder="Search topic & connector..."
        type="search"
        value={searchTerm}
      />
    </StyledToolboxSearch>
  );
};

ToolboxSearch.propTypes = {
  searchData: PropTypes.array,
  setSearchResults: PropTypes.func.isRequired,
  pipelineDispatch: PropTypes.func.isRequired,
};

export default ToolboxSearch;
