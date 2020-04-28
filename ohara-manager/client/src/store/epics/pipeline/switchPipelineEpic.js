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

import { get } from 'lodash';
import { ofType } from 'redux-observable';
import { of } from 'rxjs';
import { filter, switchMap, withLatestFrom } from 'rxjs/operators';

import { GROUP } from 'const';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';
import { hashByGroupAndName } from 'utils/sha';

export default (action$, state$, { history }) =>
  action$.pipe(
    ofType(actions.switchPipeline.TRIGGER),
    withLatestFrom(state$),
    filter(([, state]) => selectors.getWorkspaceName(state)),
    switchMap(([action, state]) => {
      const workspaceName = selectors.getWorkspaceName(state);
      const pipelineGroup = get(
        action.payload,
        'group',
        hashByGroupAndName(GROUP.WORKSPACE, workspaceName),
      );
      const pipelineName = get(action.payload, 'name');

      const targetPipeline = selectors.getPipelineById(state, {
        id: getId({ group: pipelineGroup, name: pipelineName }),
      });

      if (targetPipeline) {
        history.push(`/${workspaceName}/${targetPipeline.name}`);
        return of(actions.switchPipeline.success(targetPipeline.name));
      }

      const headPipeline = selectors.getHeadPipelineByGroup(state, {
        group: pipelineGroup,
      });

      if (headPipeline) {
        history.push(`/${workspaceName}/${headPipeline.name}`);
      } else if (workspaceName) {
        history.push(`/${workspaceName}`);
      } else {
        history.push('/');
      }

      return of(actions.switchPipeline.success(headPipeline?.name));
    }),
  );
