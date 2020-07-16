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

import { omit } from 'lodash';
import * as actions from 'store/actions';
import { ENTITY_TYPE } from 'store/schema';
import { entity, deleteEntitiesByIds } from './index';

export default function reducer(state = {}, action) {
  switch (action.type) {
    case actions.deleteStream.SUCCESS:
      return omit(state, action.payload?.streamId);
    case actions.deleteStreams.SUCCESS:
      return deleteEntitiesByIds(state, action);
    default:
      return entity(ENTITY_TYPE.streams)(state, action);
  }
}
