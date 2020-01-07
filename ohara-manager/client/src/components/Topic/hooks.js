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

import { useMemo } from 'react';
import { get } from 'lodash';
import { useTopicState } from 'context';

export const useTopics = () => {
  const { data: topics } = useTopicState();

  return useMemo(() => {
    return topics.map(topic => {
      const isShared = get(topic, 'tags.type') === 'shared';
      return {
        ...topic,
        isShared,
        displayName: isShared ? topic.name : get(topic, 'tags.displayName'),
      };
    });
  }, [topics]);
};
