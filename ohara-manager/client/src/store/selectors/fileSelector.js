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

import { values } from 'lodash';
import { createSelector } from 'reselect';
import { getDefinition } from '../../api/utils/definitionsUtils';

const getEntities = state => state?.entities?.files;

const getGroupFromProps = (_, props) => props?.group;

const getJarKeyFromProps = (_, props) => props?.jarKey;

const getClassNameFromProps = (_, props) => props?.className;

export const getFilesByGroup = createSelector(
  [getEntities, getGroupFromProps],
  (entities, group) => values(entities).filter(file => file?.group === group),
);

export const getStreamInfo = createSelector(
  [getEntities, getJarKeyFromProps, getClassNameFromProps],
  (entities, jarKey, className) => {
    const [streamInfo] = values(entities)
      .filter(
        entity => entity.group === jarKey.group && entity.name === jarKey.name,
      )
      .map(entity =>
        entity.classInfos.find(info => info.className === className),
      )
      .map(entity => {
        return {
          ...entity,
          settingDefinitions: getDefinition(entity.settingDefinitions),
        };
      });

    return streamInfo;
  },
);
