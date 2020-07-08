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
import { from } from 'rxjs';
import { switchMap, withLatestFrom } from 'rxjs/operators';

import { GROUP } from 'const';
import * as actions from 'store/actions';
import * as selectors from 'store/selectors';
import { getId } from 'utils/object';
import { isStable as isStableWorkspace } from 'utils/workspace';

export default (action$, state$, { history }) =>
  action$.pipe(
    ofType(actions.switchWorkspace.TRIGGER),
    withLatestFrom(state$),
    switchMap(([action, state]) => {
      const workspaceGroup = get(action.payload, 'group', GROUP.WORKSPACE);
      const workspaceName = get(action.payload, 'name');
      const pipelineName = get(action.payload, 'pipelineName');

      const targetWorkspace = selectors.getWorkspaceById(state, {
        id: getId({
          group: workspaceGroup,
          name: workspaceName,
        }),
      });

      if (isStableWorkspace(targetWorkspace)) {
        history.push(`/${targetWorkspace.name}`);
        return from([
          actions.switchWorkspace.success(targetWorkspace.name),
          actions.switchPipeline.trigger({ name: pipelineName }),
        ]);
      }

      const firstStableWorkspace = selectors.getFirstStableWorkspace(state);

      if (firstStableWorkspace) {
        history.push(`/${firstStableWorkspace.name}`);
      } else {
        history.push('/');
      }
      return from([
        actions.switchWorkspace.success(firstStableWorkspace?.name),
        actions.switchPipeline.trigger(),
      ]);
    }),
  );
