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
import { normalize } from 'normalizr';
import { ofType } from 'redux-observable';
import { defer } from 'rxjs';
import { map, mergeMap, startWith } from 'rxjs/operators';

import * as topicApi from 'api/topicApi';
import * as actions from 'store/actions';
import * as schema from 'store/schema';
import { getId } from 'utils/object';
import { catchErrorWithEventLog } from '../utils';

export default (action$) =>
  action$.pipe(
    ofType(actions.updateTopic.TRIGGER),
    map((action) => action.payload),
    mergeMap((values) => {
      const topicId = getId(values);
      return defer(() => topicApi.update(values)).pipe(
        map((res) => normalize(res.data, schema.topic)),
        map((normalizedData) =>
          actions.updateTopic.success(merge(normalizedData, { topicId })),
        ),
        startWith(actions.updateTopic.request({ topicId })),
        catchErrorWithEventLog((err) =>
          actions.updateTopic.failure(merge(err, { topicId })),
        ),
      );
    }),
  );
