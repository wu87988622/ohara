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

const NodeDialogContext = createContext();

const NodeDialogProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [type, setType] = useState('k8s');
  const [hasSelect, setHasSelect] = useState(false);
  const [hasSave, setHasSave] = useState(false);
  const [selected, setSelected] = useState([]);
  const reset = () => {
    setHasSelect(false);
    setHasSave(false);
  };

  return (
    <NodeDialogContext.Provider
      value={{
        isOpen,
        setIsOpen,
        type,
        setType,
        hasSelect,
        setHasSelect,
        hasSave,
        setHasSave,
        selected,
        setSelected,
        reset,
      }}
    >
      {children}
    </NodeDialogContext.Provider>
  );
};

const useNodeDialog = () => {
  const context = useContext(NodeDialogContext);

  if (context === undefined) {
    throw new Error('useNodeDialog must be used within a NodeDialogProvider');
  }

  return context;
};

NodeDialogProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export { NodeDialogProvider, useNodeDialog };
