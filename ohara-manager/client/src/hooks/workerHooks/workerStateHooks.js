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

import { useCallback, useMemo } from 'react';
import { useMappedState } from 'redux-react-hook';
import { merge } from 'lodash';

import { useApp } from 'context';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';

export const useCurrentWorkerId = () => {
  const { workerGroup: group, workerName: name } = useApp();
  return getId({ group, name });
};

export const useAllWorkers = () => {
  const getAllWorkers = useMemo(selectors.makeGetAllWorkers, []);
  const workers = useMappedState(
    useCallback(state => getAllWorkers(state), [getAllWorkers]),
  );
  return workers;
};

export const useCurrentWorker = () => {
  const id = useCurrentWorkerId();
  const getWorkerById = useMemo(selectors.makeGetWorkerById, []);
  const getInfoById = useMemo(selectors.makeGetInfoById, []);
  return useMappedState(
    useCallback(
      state => {
        const worker = getWorkerById(state, { id });
        const info = getInfoById(state, { id });
        return merge(worker, info);
      },
      [getWorkerById, getInfoById, id],
    ),
  );
};
