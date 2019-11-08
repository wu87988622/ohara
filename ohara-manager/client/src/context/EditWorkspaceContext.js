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

import React, { createContext, useContext, useState } from 'react';
import PropTypes from 'prop-types';

const EditWorkspaceContext = createContext();

const EditWorkspaceProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [tab, setTab] = useState('overview');

  return (
    <EditWorkspaceContext.Provider
      value={{
        isOpen,
        setIsOpen,
        tab,
        setTab,
      }}
    >
      {children}
    </EditWorkspaceContext.Provider>
  );
};

const useEditWorkspace = () => {
  const context = useContext(EditWorkspaceContext);

  if (context === undefined) {
    throw new Error(
      'useEditWorkspace must be used within a EditWorkspaceProvider',
    );
  }

  return context;
};

EditWorkspaceProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { EditWorkspaceProvider, useEditWorkspace };
