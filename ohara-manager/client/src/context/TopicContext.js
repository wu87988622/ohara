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

import * as topicApi from 'api/topicApi';

const TopicContext = createContext();

const TopicProvider = ({ children }) => {
  const [topics, setTopics] = useState([]);

  const fetchTopics = useCallback(async workspaceName => {
    const topics = await topicApi.getAll({ group: workspaceName });
    const sortedTopics = topics.sort((a, b) =>
      a.settings.name.localeCompare(b.settings.name),
    );

    setTopics(sortedTopics);
  }, []);

  return (
    <TopicContext.Provider
      value={{
        topics,
        doFetch: fetchTopics,
      }}
    >
      {children}
    </TopicContext.Provider>
  );
};

const useTopic = () => {
  const context = useContext(TopicContext);

  if (context === undefined) {
    throw new Error('useTopic must be used within a TopicProvider');
  }

  return context;
};

TopicProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { TopicProvider, useTopic };
