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

import { get, find, some } from 'lodash';
import {
  openDialogRoutine,
  setDialogDataRoutine,
  closeDialogRoutine,
  closePeakDialogRoutine,
  closeAllDialogRoutine,
} from 'routines/dialog';

export const createOpenDialog = (_, dispatch) => name =>
  dispatch(openDialogRoutine.trigger({ name }));

export const createIsDialogOpen = state => name =>
  some(state.dialogs, { name });

export const createSetDialogData = (_, dispatch) => (name, data) =>
  dispatch(setDialogDataRoutine.trigger({ name, data }));

export const createGetDialogData = state => name =>
  get(find(state.dialogs, { name }), 'data');

export const createCloseDialog = (_, dispatch) => name =>
  dispatch(closeDialogRoutine.trigger({ name }));

export const createClosePeakDialog = (_, dispatch) => () =>
  dispatch(closePeakDialogRoutine.trigger());

export const createCloseAllDialog = (_, dispatch) => () =>
  dispatch(closeAllDialogRoutine.trigger());
