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

import { combineEpics, ofType } from 'redux-observable';
import { map, throttleTime } from 'rxjs/operators';

import * as actions from 'store/actions';

const showMessageEpic = action$ =>
  action$.pipe(
    ofType(actions.showMessage.TRIGGER),
    // avoid message display too frequently
    throttleTime(500),
    map(action => action.payload),
    map(message => ({ message, isOpen: true })),
    map(values => actions.showMessage.success(values)),
  );

const hideMessageEpic = action$ =>
  action$.pipe(
    ofType(actions.hideMessage.TRIGGER),
    map(() => ({ message: '', isOpen: false })),
    map(values => actions.hideMessage.success(values)),
  );

export default combineEpics(showMessageEpic, hideMessageEpic);
