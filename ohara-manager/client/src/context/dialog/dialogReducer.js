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

import { dropRight, has, reject, some } from 'lodash';

import {
  openDialogRoutine,
  setDialogDataRoutine,
  closeDialogRoutine,
  closePeakDialogRoutine,
  closeAllDialogRoutine,
} from './dialogRoutines';

/**
 * The element in dialogs should be like
 *
 * ex1:
 * {
 *   name: 'EDIT_WORKSPACE_DIALOG',
 *   data: { tab: 'overview' },
 *   createdAt: 1577681120
 * }
 *
 * ex2:
 * {
 *   name: 'VIEW_TOPIC_DIALOG',
 *   data: {
 *     name: 'topic1',
 *     group: 'abc',
 *     numberOfPartitions: 1,
 *     ...
 *   },
 *   createdAt: 1577681120
 * }
 *
 * The name property is necessary and can be a string or Symbol.
 */
const initialState = {
  dialogs: [],
};

const reducer = (state, action) => {
  if (!has(action.payload, 'name')) {
    throw new Error('The payload must contain the name property');
  }
  switch (action.type) {
    case openDialogRoutine.TRIGGER:
      return {
        ...state,
        dialogs: [
          ...state.dialogs,
          { ...action.payload, createdAt: new Date() },
        ],
      };
    case setDialogDataRoutine.TRIGGER: {
      if (!some(state.dialogs, ['name', action.payload.name])) {
        throw new Error(
          'The dialog is not open, so the data cannot be set. Use the open(data) method instead.',
        );
      }
      return {
        ...state,
        dialogs: state.dialogs.map((dialog) =>
          dialog.name === action.payload.name
            ? { ...dialog, data: action.payload.data }
            : dialog,
        ),
      };
    }
    case closeDialogRoutine.TRIGGER:
      return {
        ...state,
        dialogs: reject(
          state.dialogs,
          (dialog) => dialog.name === action.payload.name,
        ),
      };
    case closePeakDialogRoutine.TRIGGER:
      return {
        ...state,
        dialogs: dropRight(state.dialogs),
      };
    case closeAllDialogRoutine.TRIGGER:
      return initialState;
    default:
      return state;
  }
};

export { reducer, initialState };
