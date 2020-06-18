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

const { QUICK, EXPERT } = CREATE_WORKSPACE_MODE;

const initialState = {
  isOpen: false,
  mode: QUICK,
  step: 0,
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
    default:
      return state;
  }
}
