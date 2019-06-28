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

import moment from 'moment';
import { useEffect, useCallback, useState } from 'react';
import { isEmpty, get, orderBy } from 'lodash';

import * as topicApi from 'api/topicApi';

export const useFetchTopics = brokerClusterName => {
  const [topics, setTopics] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const fetchTopics = useCallback(async () => {
    const res = await topicApi.fetchTopics();
    const topics = get(res, 'data.result', []);
    setIsLoading(false);

    if (!isEmpty(topics)) {
      const topicsUnderBrokerCluster = topics.filter(
        topic => topic.brokerClusterName === brokerClusterName,
      );
      setTopics(orderBy(topicsUnderBrokerCluster, 'name'));
    }
  }, [brokerClusterName]);

  useEffect(() => {
    fetchTopics();
  }, [fetchTopics]);

  return [topics, setTopics, isLoading, fetchTopics];
};

export const getDateFromTimestamp = timestamp => {
  return moment.unix(timestamp / 1000).format('YYYY-MM-DD HH:mm:ss');
};

export const getMetrics = metrics => {
  const meters = get(metrics, 'meters');

  if (!isEmpty(meters)) {
    const targetMeter = meters.find(
      meter => meter.document === 'BytesInPerSec', // Only displays this document for now
    );

    return targetMeter && `${targetMeter.value} ${targetMeter.unit}`;
  }
};
