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

import { TAB } from './const';

const DevToolContext = React.createContext();

const DevToolProvider = ({ children }) => {
  const [tabName, setTabName] = React.useState(TAB.topic);

  return (
    <DevToolContext.Provider
      value={{
        tabName,
        setTabName,
      }}
    >
      {children}
    </DevToolContext.Provider>
  );
};

const useDevTool = () => {
  const context = React.useContext(DevToolContext);
  if (context === undefined) {
    throw new Error('useDevTool must be used within a DevToolProvider');
  }
  return context;
};

DevToolProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { DevToolProvider, useDevTool };
