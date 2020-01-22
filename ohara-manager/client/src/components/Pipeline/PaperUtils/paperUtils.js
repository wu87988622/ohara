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
import { CELL_STATUS } from 'const';

export const getCellName = cell => {
  return get(cell, 'attributes.name', null);
};

export const getCellClassName = cell => {
  return get(cell, 'attributes.className', null);
};

export const getCellKind = cell => {
  return get(cell, 'attributes.kind', null);
};

export const getCellJarKey = cell => {
  return get(cell, 'attributes.jarKey', null);
};

export const getCellState = cell => {
  return get(cell, 'data.state', CELL_STATUS.stopped);
};
