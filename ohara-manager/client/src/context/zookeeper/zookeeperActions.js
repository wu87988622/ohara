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

import { isEmpty, pick, map } from 'lodash';

import * as zookeeperApi from 'api/zookeeperApi';
import * as inspectApi from 'api/inspectApi';
import { fetchZookeepersRoutine } from './zookeeperRoutines';

const fetchZookeepersCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchZookeepersRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  dispatch(routine.request());
  const zookeepers = await zookeeperApi.getAll();

  if (isEmpty(zookeepers)) {
    const error = 'failed to fetch zookeepers';
    dispatch(routine.failure(error));
    showMessage(error);
    return;
  }

  const zookeeperInfos = await Promise.all(
    map(zookeepers, async zookeeper => {
      const params = pick(zookeeper.settings, ['name', 'group']);
      const zookeeperInfo = await inspectApi.getZookeeperInfo(params);
      return { ...zookeeper, ...zookeeperInfo };
    }),
  );

  dispatch(routine.success(zookeeperInfos));
};

const addZookeeperCreator = () => async () => {
  // TODO: implement the logic for add zookeeper
};
const updateZookeeperCreator = () => async () => {
  // TODO: implement the logic for update zookeeper
};
const deleteZookeeperCreator = () => async () => {
  // TODO: implement the logic for delete zookeeper
};

export {
  fetchZookeepersCreator,
  addZookeeperCreator,
  updateZookeeperCreator,
  deleteZookeeperCreator,
};
