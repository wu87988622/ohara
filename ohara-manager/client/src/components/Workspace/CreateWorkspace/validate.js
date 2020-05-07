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

import {
  composeValidators,
  required,
  validServiceName,
  maxLength,
} from 'utils/validate';

export default values => {
  const errors = {};
  errors.workspace = {};

  errors.workspace.name = composeValidators(
    required,
    validServiceName,
    // Configurator API only accept length <= 25
    // we use the same rules here
    maxLength(25),
  )(values?.workspace?.name);

  if (values?.workspace?.nodeNames?.length <= 0) {
    errors.workspace.nodeNames = 'This is a required field';
  }
  return errors;
};
