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

import * as actions from 'store/actions';
import { ACTIONS } from 'const';

const defaultSteps = [
  'Stop Worker',
  'Stop Broker',
  'Stop Zookeeper',
  'Delete Worker',
  'Delete Broker',
  'Delete Zookeeper',
];

const initialState = {
  isOpen: false,
  loading: false,
  progress: {
    open: false,
    steps: defaultSteps,
    activeStep: 0,
    log: [],
    message: 'Start DeleteWorkspace... (0% complete)',
    isPause: false,
  },
  skipList: [],
  lastUpdated: null,
  error: null,
};

export default function reducer(state = initialState, action) {
  const now = new Date(Date.now()).toLocaleString();
  switch (action.type) {
    case actions.openDeleteWorkspace.TRIGGER:
      return {
        ...state,
        isOpen: true,
      };
    case actions.closeDeleteWorkspace.TRIGGER:
      return {
        ...state,
        isOpen: false,
      };
    case actions.pauseDeleteWorkspace.TRIGGER:
      return {
        ...state,
        progress: {
          ...state.progress,
          isPause: true,
          log: [
            ...state.progress.log,
            { title: `${now} Pause delete workspace...` },
          ],
        },
      };
    case actions.resumeDeleteWorkspace.TRIGGER:
      return {
        ...state,
        progress: {
          ...state.progress,
          isPause: true,
          log: [
            ...state.progress.log,
            { title: `${now} Resume delete workspace...` },
          ],
        },
      };
    case actions.rollbackDeleteWorkspace.TRIGGER:
      return {
        ...state,
        progress: {
          ...state.progress,
          log: [
            ...state.progress.log,
            { title: `${now} Rollback delete workspace...` },
          ],
        },
      };
    case actions.createZookeeper.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_ZOOKEEPER],
        progress: {
          ...state.progress,
          message: 'Wait create zookeeper... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Start to create zookeeper...` },
          ],
        },
      };
    case actions.startZookeeper.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.START_ZOOKEEPER],
        progress: {
          ...state.progress,
          message: 'Wait start zookeeper... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Wait to start zookeeper...` },
          ],
        },
      };
    case actions.createBroker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_BROKER],
        progress: {
          ...state.progress,
          message: 'Wait create broker... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Start to create broker...` },
          ],
        },
      };
    case actions.startBroker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.START_BROKER],
        progress: {
          ...state.progress,
          message: 'Wait start broker... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Wait to start broker...` },
          ],
        },
      };
    case actions.createWorker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_WORKER],
        progress: {
          ...state.progress,
          message: 'Wait create worker... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Start to create worker...` },
          ],
        },
      };
    case actions.startWorker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.START_WORKER],
        progress: {
          ...state.progress,
          message: 'Wait start worker... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Wait to start worker...` },
          ],
        },
      };

    case actions.stopWorker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_WORKER],
        progress: {
          ...state.progress,
          message: 'Wait stop worker... (7% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Start to stop worker...` },
          ],
        },
      };

    case actions.stopWorker.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 1,
          message: 'Stop worker success... (14% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Stop worker success...` },
          ],
        },
      };

    case actions.stopBroker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_BROKER],
        progress: {
          ...state.progress,
          message: 'Wait stop broker... (21% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Start to stop broker...` },
          ],
        },
      };

    case actions.stopBroker.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 2,
          message: 'Stop broker success... (28% complete)',
          log: [
            ...state.progress.log,
            { title: `${now} Stop broker success...` },
          ],
        },
      };

    case actions.stopZookeeper.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.STOP_ZOOKEEPER],
        progress: {
          ...state.progress,
          message: 'Wait stop zookeeper... (35% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Start to stop zookeeper...`,
            },
          ],
        },
      };

    case actions.stopZookeeper.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 3,
          message: 'Stop zookeeper success... (42% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Stop zookeeper success...`,
            },
          ],
        },
      };
    case actions.deleteWorker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.DELETE_WORKER],
        progress: {
          ...state.progress,
          message: 'Wait delete worker... (49% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Start to delete worker...`,
            },
          ],
        },
      };
    case actions.deleteWorker.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 4,
          message: 'Delete worker success... (56% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Delete worker success...`,
            },
          ],
        },
      };
    case actions.deleteBroker.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.DELETE_BROKER],
        progress: {
          ...state.progress,
          message: 'Wait delete broker... (63% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Start to delete broker...`,
            },
          ],
        },
      };
    case actions.deleteBroker.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 5,
          message: 'Delete broker success... (70% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Delete broker success...`,
            },
          ],
        },
      };
    case actions.deleteZookeeper.REQUEST:
      return {
        ...state,
        skipList: [...state.skipList, ACTIONS.DELETE_ZOOKEEPER],
        progress: {
          ...state.progress,
          message: 'Wait delete zookeeper... (77% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Start to delete zookeeper...`,
            },
          ],
        },
      };
    case actions.deleteZookeeper.SUCCESS:
      return {
        ...state,
        progress: {
          ...state.progress,
          activeStep: 6,
          message: 'Delete zookeeper success... (84% complete)',
          log: [
            ...state.progress.log,
            {
              title: `${now} Delete zookeeper success...`,
            },
          ],
        },
      };
    case actions.deleteWorkspace.REQUEST:
      return {
        ...state,
        progress: {
          ...state.progress,
          message: 'Wait delete workspace... (91% complete)',
        },
      };
    case actions.deleteWorkspace.SUCCESS:
      return {
        ...state,
        skipList: [],
        progress: {
          ...state.progress,
          activeStep: 7,
          message: 'Delete workspace success... (98% complete)',
          log: [],
        },
      };

    default:
      return state;
  }
}
