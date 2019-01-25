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

import toastr from 'toastr';
import { isString } from 'lodash';
import * as _ from './commonUtils';

export const handleError = err => {
  const message = _.get(err, 'data.errorMessage.message');
  if (isString(message)) {
    toastr.error(message);
    return;
  }

  const errorMessage = _.get(err, 'data.errorMessage');
  if (isString(errorMessage)) {
    toastr.error(errorMessage);
    return;
  }

  toastr.error(err || 'Internal Server Error');
};

export const getErrors = data => {
  const errors = data.reduce((acc, r) => {
    if (!r.pass) acc.push(r);
    return acc;
  }, []);

  return errors;
};
