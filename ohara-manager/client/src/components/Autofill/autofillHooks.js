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
import { get, replace } from 'lodash';

import * as hooks from 'hooks';

export const useSuggestiveKeys = () => {
  const currentWorker = hooks.useCurrentWorker();

  return useMemo(() => {
    const classInfos = get(currentWorker, 'classInfos', []);
    const keys = classInfos.reduce((acc, classInfo) => {
      get(classInfo, 'settingDefinitions', []).forEach(definition => {
        const { group, internal, key, permission } = definition;
        if (group !== 'core' && !internal && permission === 'EDITABLE') {
          acc.add(replace(key, /__/g, '.')); // Like "ftp__hostname" becomes "ftp.hostname");
        }
      });
      return acc;
    }, new Set([]));

    return Array.from(keys).sort();
  }, [currentWorker]);
};
