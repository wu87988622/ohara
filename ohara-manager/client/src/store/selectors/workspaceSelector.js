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

import { head, sortBy, values } from 'lodash';
import { createSelector } from 'reselect';

const getEntities = (state) => state?.entities?.workspaces;

const getIdFromProps = (_, props) => props?.id;

export const getWorkspaceName = (state) => state?.ui?.workspace?.name;

export const getAllWorkspaces = createSelector([getEntities], (entities) =>
  sortBy(values(entities), 'name'),
);

export const getWorkspaceById = createSelector(
  [getEntities, getIdFromProps],
  (entities, id) => entities[id],
);

export const getHeadWorkspace = createSelector(
  [getAllWorkspaces],
  (workspaces) => head(workspaces),
);
