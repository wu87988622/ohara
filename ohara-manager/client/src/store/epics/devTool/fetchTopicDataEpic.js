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
import { of, defer } from 'rxjs';
import { map, catchError, exhaustMap } from 'rxjs/operators';

import * as inspectApi from 'api/inspectApi';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';

export default (action$, state$) =>
  action$.pipe(
    ofType(actions.fetchDevToolTopicData.TRIGGER),
    map(action => action.payload),
    exhaustMap(values =>
      defer(() => {
        const getDevToolTopicData = selectors.makeGetDevToolTopicData();
        const topicData = getDevToolTopicData(state$.value);
        return inspectApi.getTopicData({
          key: {
            name: topicData.query.name,
            group: values.group,
          },
          limit: topicData.query.limit,
        });
      }).pipe(
        map(res => actions.fetchDevToolTopicData.success(res.data.messages)),
        catchError(res => of(actions.fetchDevToolTopicData.failure(res))),
      ),
    ),
  );
