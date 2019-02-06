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

import { CONNECTOR_TYPES } from 'constants/pipelines';
import {
  isSource,
  isSink,
  isTopic,
  isStream,
  findByGraphId,
} from '../commonUtils';

const { jdbcSource, ftpSource, hdfsSink, ftpSink, topic } = CONNECTOR_TYPES;

describe('isSource()', () => {
  it(`return false if the given kind does not contain "Source"`, () => {
    expect(isSource(topic)).toBe(false);
    expect(isSource(ftpSink)).toBe(false);
    expect(isSource(hdfsSink)).toBe(false);
  });

  it(`return true if the given kind contains "Source"`, () => {
    expect(isSource(jdbcSource)).toBe(true);
    expect(isSource(ftpSource)).toBe(true);
  });
});

describe('isSink()', () => {
  it(`return false if the given kind does not contain "Sink"`, () => {
    expect(isSink(topic)).toBe(false);
    expect(isSink(ftpSource)).toBe(false);
    expect(isSink(jdbcSource)).toBe(false);
  });

  it(`return true if the given kind contains "Sink"`, () => {
    expect(isSink(hdfsSink)).toBe(true);
    expect(isSink(ftpSink)).toBe(true);
  });
});

describe('isTopic()', () => {
  it(`return false if the given kind does not contain "topic"`, () => {
    expect(isTopic(ftpSource)).toBe(false);
    expect(isTopic(jdbcSource)).toBe(false);
    expect(isTopic(hdfsSink)).toBe(false);
    expect(isTopic(ftpSink)).toBe(false);
  });

  it(`return true if the given kind contains "topic"`, () => {
    expect(isTopic(topic)).toBe(true);
  });
});

describe('isStream()', () => {
  it(`return false if the given kind does not contain "streamApp"`, () => {
    expect(isStream(ftpSource)).toBe(false);
    expect(isStream(jdbcSource)).toBe(false);
    expect(isStream(hdfsSink)).toBe(false);
    expect(isStream(ftpSink)).toBe(false);
    expect(isStream(topic)).toBe(false);
  });

  it(`return true if the given kind contains "streamApp"`, () => {
    expect(isStream('streamApp')).toBe(true);
  });
});

describe('findByGraphId()', () => {
  it('gets the graph by its Id', () => {
    const graph = [{ id: '12345', name: 'a' }, { id: '23456', name: 'b' }];
    const result = findByGraphId(graph, graph[1].id);
    expect(result).toBe(graph[1]);
  });

  it(`returns undefined empty array if there's no match`, () => {
    const graph = [{ id: '12345', name: 'a' }, { id: '23456', name: 'b' }];
    const result = findByGraphId(graph, '44444');
    expect(result).toBeUndefined();
  });
});
