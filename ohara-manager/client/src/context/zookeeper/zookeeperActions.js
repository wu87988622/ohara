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

import { has, map } from 'lodash';

import * as zookeeperApi from 'api/zookeeperApi';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import {
  fetchZookeepersRoutine,
  addZookeeperRoutine,
  updateStagingSettingsRoutine,
} from './zookeeperRoutines';
import { getKey } from 'utils/object';

const ZOOKEEPER = 'zookeeper';

const checkRequired = values => {
  if (!has(values, 'name')) {
    throw new Error("Values is missing required member 'name'");
  }
};

const transformToZookeeper = (zookeeper, zookeeperInfo, stagingZookeeper) => {
  if (!has(zookeeper, 'settings')) {
    throw new Error("zookeeper is missing required member 'settings'");
  }
  if (!has(zookeeperInfo, 'settingDefinitions')) {
    throw new Error(
      "zookeeperInfo is missing required member 'settingDefinitions'",
    );
  }
  return {
    serviceType: ZOOKEEPER,
    ...zookeeper,
    ...zookeeperInfo,
    stagingSettings: stagingZookeeper,
  };
};

const fetchZookeepersCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchZookeepersRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  try {
    dispatch(routine.request());

    const resultForFetchZookeepers = await zookeeperApi.getAll();
    if (resultForFetchZookeepers.errors) {
      throw new Error(resultForFetchZookeepers.title);
    }

    const zookeepers = await Promise.all(
      map(resultForFetchZookeepers.data, async zookeeper => {
        const key = getKey(zookeeper);

        const resultForFetchZookeeperInfo = await inspectApi.getZookeeperInfo(
          key,
        );
        if (resultForFetchZookeeperInfo.errors) {
          throw new Error(resultForFetchZookeeperInfo.title);
        }

        const resultForFetchStagingZookeeper = await objectApi.get(key);
        if (resultForFetchStagingZookeeper.errors) {
          throw new Error(resultForFetchStagingZookeeper.title);
        }

        return transformToZookeeper(
          zookeeper,
          resultForFetchZookeeperInfo.data,
          resultForFetchStagingZookeeper.data,
        );
      }),
    );
    dispatch(routine.success(zookeepers));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const addZookeeperCreator = (
  state,
  dispatch,
  showMessage,
  routine = addZookeeperRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const ensuredValues = { ...values, group: ZOOKEEPER };
    dispatch(routine.request());

    const resultForCreateZookeeper = await zookeeperApi.create(ensuredValues);
    if (resultForCreateZookeeper.errors) {
      throw new Error(resultForCreateZookeeper.title);
    }

    const resultForStartZookeeper = await zookeeperApi.start(ensuredValues);
    if (resultForStartZookeeper.errors) {
      throw new Error(resultForStartZookeeper.title);
    }

    const resultForStageZookeeper = await objectApi.create(ensuredValues);
    if (resultForStageZookeeper.errors) {
      throw new Error(resultForStageZookeeper.title);
    }

    const resultForFetchZookeeperInfo = await inspectApi.getZookeeperInfo(
      ensuredValues,
    );
    if (resultForFetchZookeeperInfo.errors) {
      throw new Error(resultForFetchZookeeperInfo.title);
    }

    dispatch(
      routine.success(
        transformToZookeeper(
          resultForCreateZookeeper.data,
          resultForFetchZookeeperInfo.data,
          resultForStageZookeeper.data,
        ),
      ),
    );
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const updateZookeeperCreator = () => async () => {
  // TODO: implement the logic for update zookeeper
};
const deleteZookeeperCreator = () => async () => {
  // TODO: implement the logic for delete zookeeper
};

const updateStagingSettingsCreator = (
  state,
  dispatch,
  showMessage,
  routine = updateStagingSettingsRoutine,
) => async params => {
  if (state.isFetching) return;

  dispatch(routine.request());
  const result = await objectApi.update(params);

  if (result.errors) {
    dispatch(routine.failure(result.title));
    showMessage(result.title);
    return;
  }

  dispatch(routine.success(result.data));
};

export {
  fetchZookeepersCreator,
  addZookeeperCreator,
  updateZookeeperCreator,
  deleteZookeeperCreator,
  updateStagingSettingsCreator,
};
