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
import { get, map, find, isEmpty, split } from 'lodash';
import * as context from 'context';
import { usePrevious } from 'utils/hooks';

export const useCurrentLogs = () => {
  const { data, query } = context.useLogState();
  const { logs: allLogs } = data;
  const { hostName } = query;

  return useMemo(() => {
    const log = get(find(allLogs, { hostname: hostName }), 'value');
    return isEmpty(log) ? [] : split(log, '\n');
  }, [allLogs, hostName]);
};

export const useCurrentHosts = () => {
  const { data } = context.useLogState();
  const { logs: allLogs } = data;

  return useMemo(() => {
    return map(allLogs, 'hostname');
  }, [allLogs]);
};

export const useCurrentHostName = () => {
  const { query, data } = context.useLogState();
  const { logs: allLogs } = data;
  const { hostName } = query;
  const prevAllLogs = usePrevious(allLogs);

  return useMemo(() => {
    if (!isEmpty(allLogs) && prevAllLogs === allLogs) return hostName;
    else {
      if (isEmpty(allLogs)) return '';
      else return allLogs[0].hostname;
    }
  }, [prevAllLogs, allLogs, hostName]);
};
