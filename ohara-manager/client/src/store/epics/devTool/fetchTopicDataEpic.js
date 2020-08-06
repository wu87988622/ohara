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
import { defer } from 'rxjs';
import { map, exhaustMap } from 'rxjs/operators';

import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { catchErrorWithEventLog } from '../utils';

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.fetchDevToolTopicData.TRIGGER),
    map((action) => action.payload),
    exhaustMap((values) =>
      defer(() => {
        const getDevToolTopicData = selectors.makeGetDevToolTopicData();
        const topicData = getDevToolTopicData(state$.value);
        const topicName = topicData.query.name;
        return inspectApi.getTopicData({
          key: {
            name: topicName,
            group: values.group,
          },
          limit: topicData.query.limit,
        });
      }).pipe(
        map((res) => actions.fetchDevToolTopicData.success(res.data.messages)),
        catchErrorWithEventLog((err) =>
          actions.fetchDevToolTopicData.failure(err),
        ),
      ),
    ),
  );
