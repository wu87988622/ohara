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

const ViewTopicContext = createContext();

const ViewTopicProvider = ({ children }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [topic, setTopic] = useState();

  return (
    <ViewTopicContext.Provider
      value={{
        isOpen,
        setIsOpen,
        topic,
        setTopic,
      }}
    >
      {children}
    </ViewTopicContext.Provider>
  );
};

const useViewTopic = () => {
  const context = useContext(ViewTopicContext);

  if (context === undefined) {
    throw new Error('useViewTopic must be used within a ViewTopicProvider');
  }

  return context;
};

ViewTopicProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { ViewTopicProvider, useViewTopic };
