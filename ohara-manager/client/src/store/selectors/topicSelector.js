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

import { filter, map, some, sortBy, values } from 'lodash';
import { createSelector } from 'reselect';
import { findPipelinesByGroup } from './pipelineSelector';

const getEntities = (state) => state?.entities?.topics;

const getGroupFromProps = (_, props) => props?.group;

const getIdFromProps = (_, props) => props?.id;

export const getTopicById = createSelector(
  [getEntities, getIdFromProps],
  (entities, id) => entities[id],
);

export const getAllTopics = createSelector([getEntities], (entities) =>
  sortBy(values(entities), 'name'),
);

export const getTopicsByGroup = createSelector(
  [getAllTopics, getGroupFromProps, findPipelinesByGroup],
  (allTopics, group, pipelines) => {
    const topics = filter(allTopics, (topic) => topic?.group === group);
    return map(topics, (topic) => {
      return {
        ...topic,
        pipelines: filter(pipelines, (pipeline) => {
          return some(
            pipeline?.objects,
            (object) => object.name === topic.name,
          );
        }),
      };
    });
  },
);

export const getSharedTopicsByGroup = createSelector(
  [getTopicsByGroup],
  (topics) => filter(topics, (topic) => topic?.isShared === true),
);

export const getPipelineOnlyTopicsByGroup = createSelector(
  [getTopicsByGroup],
  (topics) => filter(topics, (topic) => topic?.isShared === false),
);
