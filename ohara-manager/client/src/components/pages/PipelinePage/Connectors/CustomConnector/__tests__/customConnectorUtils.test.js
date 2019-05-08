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

import * as generate from 'utils/generate';
import { getCurrTopicId, getCurrTopicName } from '../customConnectorUtils';

describe('getCurrTopicId()', () => {
  it(`doesn't do anything if either originals or target are negative values`, () => {
    expect(getCurrTopicId({ originals: [] })).toBe(undefined);
    expect(getCurrTopicId({})).toBe(undefined);
  });

  it('gets the right topic id', () => {
    const topicName = generate.name();
    const topicId = generate.id();

    const originals = [
      {
        name: topicName,
        id: topicId,
      },
      {
        name: generate.name(),
        id: generate.id(),
      },
      {
        name: generate.name(),
        id: generate.id(),
      },
    ];

    expect(getCurrTopicId({ originals, target: topicName })).toEqual([topicId]);
  });
});

describe('getCurrTopicName()', () => {
  it('gets the right topic id', () => {
    const topicName = generate.name();
    const topicId = generate.id();

    const originals = [
      {
        name: topicName,
        id: topicId,
      },
      {
        name: generate.name(),
        id: generate.id(),
      },
      {
        name: generate.name(),
        id: generate.id(),
      },
    ];

    expect(getCurrTopicName({ originals, target: [topicId] })).toEqual(
      topicName,
    );
  });
});
