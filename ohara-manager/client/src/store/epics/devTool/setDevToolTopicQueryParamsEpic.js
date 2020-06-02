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

import { ofType } from 'redux-observable';
import { of, asapScheduler, scheduled, queueScheduler, from } from 'rxjs';
import {
  map,
  catchError,
  concatMap,
  switchMap,
  mergeAll,
  filter,
  mapTo,
} from 'rxjs/operators';

import { LOG_LEVEL } from 'const';
import * as actions from 'store/actions';

export default (action$) =>
  action$.pipe(
    ofType(actions.setDevToolTopicQueryParams.TRIGGER),
    map((action) => action.payload),
    concatMap((values) =>
      scheduled(
        [
          of(values).pipe(
            filter(() => !!values.params.limit),
            mapTo(
              actions.setDevToolTopicQueryParams.success({
                limit: values.params.limit,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.name),
            switchMap(() =>
              scheduled(
                [
                  actions.setDevToolTopicQueryParams.success({
                    name: values.params.name,
                  }),
                  actions.fetchDevToolTopicData.trigger({
                    group: values.topicGroup,
                    topics: values.topics,
                  }),
                ],
                asapScheduler,
              ),
            ),
          ),
        ],
        queueScheduler,
      ).pipe(
        mergeAll(),
        catchError((err) =>
          from([
            actions.setDevToolTopicQueryParams.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
