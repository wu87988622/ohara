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

import React, { createContext, useContext, useState, useCallback } from 'react';
import PropTypes from 'prop-types';

import * as pipelineApi from 'api/pipelineApi';

const PipelineContext = createContext();

const PipelineProvider = ({ children }) => {
  const [pipelines, setPipelines] = useState([]);
  const [isFetching, setIsFetching] = useState(false);

  const fetchPipelines = useCallback(async workspaceName => {
    setIsFetching(true);
    const response = await pipelineApi.getAll({ group: workspaceName });
    const pipelines = response.sort((a, b) => a.name.localeCompare(b.name));
    setIsFetching(false);
    setPipelines(pipelines);
  }, []);

  return (
    <PipelineContext.Provider
      value={{
        pipelines,
        doFetch: fetchPipelines,
        isFetching,
      }}
    >
      {children}
    </PipelineContext.Provider>
  );
};

const usePipeline = () => {
  const context = useContext(PipelineContext);

  if (context === undefined) {
    throw new Error('usePipeline must be used within a PipelineProvider');
  }

  return context;
};

PipelineProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { PipelineProvider, usePipeline };
