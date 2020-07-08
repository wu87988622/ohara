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

import { isShabondi } from '../PipelineUtils';
import { SOURCE, SINK } from 'api/apiInterface/connectorInterface';

describe('isShabondi', () => {
  it('should return false when not matching', () => {
    expect(isShabondi('abc')).toBe(false);
    expect(isShabondi(SOURCE.jdbc)).toBe(false);
    expect(isShabondi(SINK.ftp)).toBe(false);
  });

  it('should return true when className is matched', () => {
    expect(isShabondi(SOURCE.shabondi)).toBe(true);
    expect(isShabondi(SINK.shabondi)).toBe(true);
  });
});
