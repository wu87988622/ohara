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

import { useEffect, useState } from 'react';
import * as topicApi from 'api/topicApi';
import { isEmpty, get, orderBy } from 'lodash';

export const useFetchTopics = brokerClusterName => {
  const [topics, setTopics] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchTopics = async () => {
    const res = await topicApi.fetchTopics();
    const topics = get(res, 'data.result', []);
    setIsLoading(false);

    if (!isEmpty(topics)) {
      const topicsUnderBrokerCluster = topics.filter(
        topic => topic.brokerClusterName === brokerClusterName,
      );
      setTopics(orderBy(topicsUnderBrokerCluster, 'name'));
    }
  };

  useEffect(() => {
    fetchTopics();
    // Disable the eslint warning for now. As it's very default to
    // understand what's really going on here.
  }, []); // eslint-disable-line

  return [topics, setTopics, isLoading, fetchTopics];
};
