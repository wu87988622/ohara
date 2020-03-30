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

import { includes, isSafeInteger } from 'lodash';
import * as actions from 'store/actions';
import { CREATE_WORKSPACE_MODE } from 'const';

const defaultSteps = [
  'Create Zookeeper',
  'Create Broker',
  'Create Worker',
  'Start Zookeeper',
  'Start Broker',
  'Start Worker',
];

const { QUICK, EXPERT } = CREATE_WORKSPACE_MODE;

const initialState = {
  isOpen: false,
  mode: QUICK,
  step: 0,
  loading: false,
  progress: {
    open: false,
    steps: defaultSteps,
    activeStep: 0,
  },
  lastUpdated: null,
  error: null,
};

export default function reducer(state = initialState, action) {
  switch (action.type) {
    case actions.openCreateWorkspace.TRIGGER:
      return {
        ...state,
        isOpen: true,
      };
    case actions.closeCreateWorkspace.TRIGGER:
      return {
        ...state,
        isOpen: false,
      };
    case actions.switchCreateWorkspaceMode.TRIGGER:
      return {
        ...state,
        mode: includes([QUICK, EXPERT], action.payload)
          ? action.payload
          : QUICK,
      };
    case actions.switchCreateWorkspaceStep.TRIGGER:
      return {
        ...state,
        step: isSafeInteger(action.payload) ? action.payload : 0,
      };
    case actions.createWorkspace.TRIGGER:
      return {
        ...state,
        loading: true,
        progress: { ...state.progress, open: true },
        error: null,
      };
    case actions.createWorkspace.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 0 },
      };

    case actions.createZookeeper.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 1 },
      };
    case actions.createBroker.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 2 },
      };
    case actions.createWorker.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 3 },
      };
    case actions.startZookeeper.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 4 },
      };
    case actions.startBroker.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 5 },
      };
    case actions.startWorker.SUCCESS:
      return {
        ...state,
        progress: { ...state.progress, activeStep: 6 },
      };

    case actions.createWorkspace.FAILURE:
      return {
        ...state,
        loading: false,
        progress: { ...state.progress, open: false },
        error: action.payload,
      };
    case actions.createWorkspace.FULFILL:
      return {
        ...state,
        loading: false,
        progress: { ...state.progress, open: false },
        lastUpdated: new Date(),
      };
    default:
      return state;
  }
}
