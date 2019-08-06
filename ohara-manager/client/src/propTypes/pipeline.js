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

import { string, arrayOf, shape, bool, number, any } from 'prop-types';

export const graph = shape({
  name: string.isRequired,
  isActive: bool,
  metrics: shape({
    meters: arrayOf(
      shape({
        document: string,
        unit: string,
        value: number,
      }),
    ),
  }),
  className: string.isRequired,
  kind: string.isRequired,
  to: any.isRequired,
});

export const topic = shape({
  name: string.isRequired,
});

export const definition = shape({
  defaultValue: string,
  displayName: string.isRequired,
  documentation: string.isRequired,
  editable: bool.isRequired,
  group: string.isRequired,
  internal: bool.isRequired,
  key: string.isRequired,
  orderInGroup: number.isRequired,
  reference: string.isRequired,
  required: bool.isRequired,
  tableKeys: arrayOf(string),
  valueType: string.isRequired,
});
