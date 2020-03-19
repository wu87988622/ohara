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

export const useCurrentZookeeperId = () => {
  const { zookeeperGroup: group, zookeeperName: name } = useApp();
  return getId({ group, name });
};

export const useAllZookeepers = () => {
  const getAllZookeepers = useMemo(selectors.makeGetAllZookeepers, []);
  const zookeepers = useMappedState(
    useCallback(state => getAllZookeepers(state), [getAllZookeepers]),
  );
  return zookeepers;
};

export const useCurrentZookeeper = () => {
  const id = useCurrentZookeeperId();
  const getZookeeperById = useMemo(selectors.makeGetZookeeperById, []);
  const getInfoById = useMemo(selectors.makeGetInfoById, []);
  return useMappedState(
    useCallback(
      state => {
        const zookeeper = getZookeeperById(state, { id });
        const info = getInfoById(state, { id });
        return merge(zookeeper, info);
      },
      [getZookeeperById, getInfoById, id],
    ),
  );
};
