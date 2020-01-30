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

function ContextApiError({ title, status, errors, meta }) {
  this.name = 'ContextApiError';
  this.message = title;
  this.title = title;
  this.status = status;
  this.errors = errors;
  this.meta = meta;
  this.stack = new Error().stack;
}

ContextApiError.prototype = new Error();
ContextApiError.prototype.constructor = ContextApiError;

ContextApiError.prototype.getPayload = function() {
  const payload = {};
  if (this.title) payload.title = this.title;
  if (this.status) payload.status = this.status;
  if (this.errors) payload.errors = this.errors;
  if (this.meta) payload.meta = this.meta;
  return payload;
};

export default ContextApiError;
