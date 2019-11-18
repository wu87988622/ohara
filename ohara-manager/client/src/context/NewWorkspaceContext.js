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

const NewWorkspaceContext = createContext();

const NewWorkspaceProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <NewWorkspaceContext.Provider
      value={{
        isOpen,
        setIsOpen,
      }}
    >
      {children}
    </NewWorkspaceContext.Provider>
  );
};

const useNewWorkspace = () => {
  const context = useContext(NewWorkspaceContext);

  if (context === undefined) {
    throw new Error(
      'useNewWorkspace must be used within a NewWorkspaceProvider',
    );
  }

  return context;
};

NewWorkspaceProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { NewWorkspaceProvider, useNewWorkspace };
