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

import { createSelector } from 'reselect';
import { transformDef } from 'utils/definition';

const getEntities = state => state?.entities?.infos;

const getIdFromProps = (_, props) => props?.id;

export const getInfoById = createSelector(
  [getEntities, getIdFromProps],
  (entities, id) => {
    const entity = entities[id];

    if (!entity) return;

    const newClassInfos = entity.classInfos.map(info => {
      return {
        ...info,
        settingDefinitions: transformDef(info.settingDefinitions),
      };
    });

    return {
      ...entity,
      classInfos: newClassInfos,
      settingDefinitions: transformDef(entity.settingDefinitions),
    };
  },
);
