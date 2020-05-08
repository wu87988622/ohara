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
  switchMap,
  mergeAll,
  filter,
  mapTo,
  concatMap,
} from 'rxjs/operators';

import { KIND, LOG_LEVEL } from 'const';
import * as actions from 'store/actions';

export default action$ =>
  action$.pipe(
    ofType(actions.setDevToolLogQueryParams.TRIGGER),
    map(action => action.payload),
    concatMap(values =>
      scheduled(
        [
          of(values).pipe(
            filter(() => !!values.params.hostName),
            mapTo(
              actions.setDevToolLogQueryParams.success({
                hostName: values.params.hostName,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.timeGroup),
            mapTo(
              actions.setDevToolLogQueryParams.success({
                timeGroup: values.params.timeGroup,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.timeRange),
            mapTo(
              actions.setDevToolLogQueryParams.success({
                timeRange: values.params.timeRange,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.startTime),
            mapTo(
              actions.setDevToolLogQueryParams.success({
                startTime: values.params.startTime,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.endTime),
            mapTo(
              actions.setDevToolLogQueryParams.success({
                endTime: values.params.endTime,
              }),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.shabondiName),
            switchMap(() =>
              scheduled(
                [
                  // provide an initial value when switching to stream
                  actions.setDevToolLogQueryParams.success({ hostName: '' }),
                  actions.setDevToolLogQueryParams.success({
                    shabondiKey: {
                      name: values.params.shabondiName,
                      group: values.shabondiGroup,
                    },
                  }),
                  actions.fetchDevToolLog.trigger(),
                ],
                asapScheduler,
              ),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.streamName),
            switchMap(() =>
              scheduled(
                [
                  // provide an initial value when switching to stream
                  actions.setDevToolLogQueryParams.success({ hostName: '' }),
                  actions.setDevToolLogQueryParams.success({
                    streamKey: {
                      name: values.params.streamName,
                      group: values.streamGroup,
                    },
                  }),
                  actions.fetchDevToolLog.trigger(),
                ],
                asapScheduler,
              ),
            ),
          ),
          of(values).pipe(
            filter(() => !!values.params.logType),
            switchMap(() => {
              const outActions = [
                actions.setDevToolLogQueryParams.success({
                  logType: values.params.logType,
                }),
                // initial the hostName value
                actions.setDevToolLogQueryParams.success({
                  hostName: '',
                }),
              ];
              if (values.params.logType !== KIND.stream)
                outActions.push(actions.fetchDevToolLog.trigger());
              return scheduled(outActions, asapScheduler);
            }),
          ),
        ],
        queueScheduler,
      ).pipe(
        mergeAll(),
        catchError(err =>
          from([
            actions.setDevToolLogQueryParams.failure(err),
            actions.createEventLog.trigger({ ...err, type: LOG_LEVEL.error }),
          ]),
        ),
      ),
    ),
  );
