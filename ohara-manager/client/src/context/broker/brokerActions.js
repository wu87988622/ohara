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

import * as brokerApi from 'api/brokerApi';
import * as inspectApi from 'api/inspectApi';
import * as objectApi from 'api/objectApi';
import {
  fetchBrokersRoutine,
  addBrokerRoutine,
  stageBrokerRoutine,
} from './brokerRoutines';
import { getKey, findByGroupAndName } from 'utils/object';

const BROKER = 'broker';

const checkRequired = values => {
  if (!has(values, 'name')) {
    throw new Error("Values is missing required member 'name'");
  }
};

const transformToStagingBroker = object => ({
  settings: omit(object, 'tags'),
  stagingSettings: get(object, 'tags'),
});

const combineBroker = (broker, brokerInfo, stagingBroker) => {
  if (!has(broker, 'settings')) {
    throw new Error("broker is missing required member 'settings'");
  }
  if (!has(brokerInfo, 'settingDefinitions')) {
    throw new Error(
      "brokerInfo is missing required member 'settingDefinitions'",
    );
  }
  if (!has(stagingBroker, 'stagingSettings')) {
    throw new Error(
      "stagingBroker is missing required member 'stagingSettings'",
    );
  }
  return {
    serviceType: BROKER,
    ...broker,
    ...brokerInfo,
    stagingSettings: stagingBroker.stagingSettings,
  };
};

const fetchBrokersCreator = (
  state,
  dispatch,
  showMessage,
  routine = fetchBrokersRoutine,
) => async () => {
  if (state.isFetching || state.lastUpdated || state.error) return;

  try {
    dispatch(routine.request());

    const resultForFetchBrokers = await brokerApi.getAll();
    if (resultForFetchBrokers.errors) {
      throw new Error(resultForFetchBrokers.title);
    }

    const brokers = await Promise.all(
      map(resultForFetchBrokers.data, async broker => {
        const key = getKey(broker);

        const resultForFetchBrokerInfo = await inspectApi.getBrokerInfo(key);
        if (resultForFetchBrokerInfo.errors) {
          throw new Error(resultForFetchBrokerInfo.title);
        }

        const resultForFetchStagingBroker = await objectApi.get(key);
        if (resultForFetchStagingBroker.errors) {
          throw new Error(resultForFetchStagingBroker.title);
        }

        const brokerInfo = resultForFetchBrokerInfo.data;
        const stagingBroker = transformToStagingBroker(
          resultForFetchStagingBroker.data,
        );
        return combineBroker(broker, brokerInfo, stagingBroker);
      }),
    );
    dispatch(routine.success(brokers));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const addBrokerCreator = (
  state,
  dispatch,
  showMessage,
  routine = addBrokerRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const ensuredValues = { ...values, group: BROKER };
    dispatch(routine.request());

    const resultForCreateBroker = await brokerApi.create(ensuredValues);
    if (resultForCreateBroker.errors) {
      throw new Error(resultForCreateBroker.title);
    }

    const resultForStartBroker = await brokerApi.start(ensuredValues);
    if (resultForStartBroker.errors) {
      throw new Error(resultForStartBroker.title);
    }

    const broker = resultForCreateBroker.data;
    const settings = broker.settings;
    const stagingData = { ...settings, tags: omit(settings, 'tags') };

    const resultForStageBroker = await objectApi.create(stagingData);
    if (resultForStageBroker.errors) {
      throw new Error(resultForStageBroker.title);
    }

    const resultForFetchBrokerInfo = await inspectApi.getBrokerInfo(
      ensuredValues,
    );
    if (resultForFetchBrokerInfo.errors) {
      throw new Error(resultForFetchBrokerInfo.title);
    }

    const brokerInfo = resultForFetchBrokerInfo.data;
    const stagingBroker = transformToStagingBroker(resultForStageBroker.data);
    dispatch(routine.success(combineBroker(broker, brokerInfo, stagingBroker)));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

const updateBrokerCreator = () => async () => {
  // TODO: implement the logic for update broker
};

const deleteBrokerCreator = () => async () => {
  // TODO: implement the logic for delete broker
};

const stageBrokerCreator = (
  state,
  dispatch,
  showMessage,
  routine = stageBrokerRoutine,
) => async values => {
  if (state.isFetching) return;

  try {
    checkRequired(values);
    const group = BROKER;
    const name = values.name;
    const targetBroker = findByGroupAndName(state.data, group, name);
    const ensuredValues = {
      name,
      group,
      tags: {
        ...omit(targetBroker.settings, 'tags'),
        ...omit(targetBroker.stagingSettings, 'tags'),
        ...omit(values, 'tags'),
      },
    };

    const resultForStageBroker = await objectApi.update(ensuredValues);
    if (resultForStageBroker.errors) {
      throw new Error(resultForStageBroker.title);
    }

    const stagingBroker = transformToStagingBroker(resultForStageBroker.data);
    dispatch(routine.success(stagingBroker));
  } catch (e) {
    dispatch(routine.failure(e.message));
    showMessage(e.message);
  }
};

export {
  fetchBrokersCreator,
  addBrokerCreator,
  updateBrokerCreator,
  deleteBrokerCreator,
  stageBrokerCreator,
};
