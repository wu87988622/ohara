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

const AddTopicContext = createContext();

const AddTopicProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <AddTopicContext.Provider
      value={{
        isOpen,
        setIsOpen,
      }}
    >
      {children}
    </AddTopicContext.Provider>
  );
};

const useAddTopic = () => {
  const context = useContext(AddTopicContext);

  if (context === undefined) {
    throw new Error('useAddTopic must be used within a AddTopicProvider');
  }

  return context;
};

AddTopicProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { AddTopicProvider, useAddTopic };
