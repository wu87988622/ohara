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

const isEmptyStr = val => val.length === 0;

const isEmptyArr = arr => arr.length === 0;

const isDefined = val => typeof val !== 'undefined';

const isNumber = val => typeof val === 'number';

const reduceByProp = (data, prop) => {
  const result = data.reduce((prev, curr) =>
    prev[prop] > curr[prop] ? prev : curr,
  );

  return result;
};

export { reduceByProp, isEmptyStr, isEmptyArr, isDefined, isNumber };
