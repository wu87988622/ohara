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

import { get, has, map, omit } from 'lodash';

import * as zookeeperApi from 'api/zookeeperApi';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import {
  fetchZookeepersRoutine,
  addZookeeperRoutine,
  stageZookeeperRoutine,
} from './zookeeperRoutines';
import { getKey, findByGroupAndName } from 'utils/object';

const ZOOKEEPER = 'zookeeper';

const checkRequired = values => {
  if (!has(values, 'name')) {
    throw new Error("Values is missing required member 'name'");
  }
};

const transformToStagingZookeeper = object => ({
  settings: omit(object, 'tags'),
  stagingSettings: get(object, 'tags'),
});

const combineZookeeper = (zookeeper, zookeeperInfo, stagingZookeeper) => {
  if (!has(zookeeper, 'settings')) {
    throw new Error("zookeeper is missing required member 'settings'");
  }
  if (!has(zookeeperInfo, 'settingDefinitions')) {
    throw new Error(
      "zookeeperInfo is missing required member 'settingDefinitions'",
    );
  }
  if (!has(stagingZookeeper, 'stagingSettings')) {
    throw new Error(
      "stagingZookeeper is missing required member 'stagingSettings'",
    );
  }
  return {
    serviceType: ZOOKEEPER,
    ...zookeeper,
    ...zookeeperInfo,
    stagingSettings: stagingZookeeper.stagingSettings,
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

        const zookeeperInfo = resultForFetchZookeeperInfo.data;
        const stagingZookeeper = transformToStagingZookeeper(
          resultForFetchStagingZookeeper.data,
        );
        return combineZookeeper(zookeeper, zookeeperInfo, stagingZookeeper);
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

    const zookeeper = resultForCreateZookeeper.data;
    const settings = zookeeper.settings;
    const stagingData = { ...settings, tags: omit(settings, 'tags') };

    const resultForStageZookeeper = await objectApi.create(stagingData);
    if (resultForStageZookeeper.errors) {
      throw new Error(resultForStageZookeeper.title);
    }

    const resultForFetchZookeeperInfo = await inspectApi.getZookeeperInfo(
      ensuredValues,
    );
    if (resultForFetchZookeeperInfo.errors) {
      throw new Error(resultForFetchZookeeperInfo.title);
    }

    const zookeeperInfo = resultForFetchZookeeperInfo.data;
    const stagingZookeeper = transformToStagingZookeeper(
      resultForStageZookeeper.data,
    );
    dispatch(
      routine.success(
        combineZookeeper(zookeeper, zookeeperInfo, stagingZookeeper),
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

const stageZookeeperCreator = (
  state,
  dispatch,
  showMessage,
  routine = stageZookeeperRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const group = ZOOKEEPER;
    const name = values.name;
    const targetZookeeper = findByGroupAndName(state.data, group, name);
    const ensuredValues = {
      name,
      group,
      tags: {
        ...omit(targetZookeeper.settings, 'tags'),
        ...omit(targetZookeeper.stagingSettings, 'tags'),
        ...omit(values, 'tags'),
      },
    };

    const resultForStageZookeeper = await objectApi.update(ensuredValues);
    if (resultForStageZookeeper.errors) {
      throw new Error(resultForStageZookeeper.title);
    }

    const stagingZookeeper = transformToStagingZookeeper(
      resultForStageZookeeper.data,
    );
    dispatch(routine.success(stagingZookeeper));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

export {
  fetchZookeepersCreator,
  addZookeeperCreator,
  updateZookeeperCreator,
  deleteZookeeperCreator,
  stageZookeeperCreator,
};
