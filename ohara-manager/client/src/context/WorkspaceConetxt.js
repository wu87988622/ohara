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

import React, { createContext, useContext, useState, useEffect } from 'react';
import PropTypes from 'prop-types';

import * as workerApi from 'api/workerApi';

const WorkspaceContext = createContext();

const WorkspaceProvider = ({ children }) => {
  const [workspaces, setWorkspaces] = useState([]);

  useEffect(() => {
    const fetchWorkers = async () => {
      const response = await workerApi.getAll();
      setWorkspaces(response);
    };

    fetchWorkers();
  }, []);

  const sortedWorkspaces = workspaces.sort((a, b) =>
    a.settings.name.localeCompare(b.settings.name),
  );

  return (
    <WorkspaceContext.Provider
      value={{
        workspaces: sortedWorkspaces,
        unsortedWorkspaces: workspaces,
      }}
    >
      {children}
    </WorkspaceContext.Provider>
  );
};

const useWorkspace = () => {
  const context = useContext(WorkspaceContext);

  if (context === undefined) {
    throw new Error('useWorkspace must be used within a WorkspaceProvider');
  }

  return context;
};

WorkspaceProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { WorkspaceProvider, useWorkspace };
