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

import { merge } from 'lodash';
import { ofType } from 'redux-observable';
import { defer, from } from 'rxjs';
import { map, mergeMap, distinctUntilChanged, startWith } from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

// Note: The caller SHOULD handle the error of this action
export const deleteTopic$ = (params) => {
  const topicId = getId(params);
  return defer(() => topicApi.remove(params)).pipe(
    mergeMap(() => {
      return from([
        actions.setSelectedCell.trigger(null),
        actions.deleteTopic.success({ topicId }),
      ]);
    }),
    startWith(actions.deleteTopic.request({ topicId })),
  );
};

export default (action$) =>
  action$.pipe(
    ofType(actions.deleteTopic.TRIGGER),
    map((action) => action.payload),
    distinctUntilChanged(),
    mergeMap((params) =>
      deleteTopic$(params).pipe(
        catchErrorWithEventLog((err) =>
          actions.deleteTopic.failure(merge(err, { topicId: getId(params) })),
        ),
      ),
    ),
  );
