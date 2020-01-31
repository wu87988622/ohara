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
import { CELL_STATUS, KIND } from 'const';

export const CONNECTION_TYPE = {
  SOURCE_TOPIC_SINK: 'source_topic_sink',
  SOURCE_TOPIC_STREAM: 'source_topic_stream',
  STREAM_TOPIC_SINK: 'stream_topic_sink',
  SOURCE_TOPIC: 'source_topic',
  STREAM_TOPIC: 'stream_topic',
  TOPIC_SINK: 'topic_sink',
  TOPIC_STREAM: 'topic_stream',
};

export const getCellState = cell => {
  return get(cell, 'data.state', CELL_STATUS.stopped);
};

export const getConnectionOrder = cells => {
  const hasSource = get(cells, 'sourceElement.kind', null) === KIND.source;
  const hasSink = get(cells, 'targetElement.kind', null) === KIND.sink;
  const hasStream =
    get(cells, 'sourceElement.kind', null) === KIND.stream ||
    get(cells, 'targetElement.kind', null) === KIND.stream;

  if (hasSource && hasSink && !hasStream) {
    const topic = get(cells, 'topicElement', null);
    const source = get(cells, 'sourceElement', null);
    const sink = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.SOURCE_TOPIC_SINK,
      topic,
      source,
      sink,
      firstLink,
      secondeLink,
    };
  }
  if (hasSource && !hasSink && hasStream) {
    const topic = get(cells, 'topicElement', null);
    const source = get(cells, 'sourceElement', null);
    const stream = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.SOURCE_TOPIC_STREAM,
      topic,
      source,
      stream,
      firstLink,
      secondeLink,
    };
  }
  if (!hasSource && hasSink && hasStream) {
    const topic = get(cells, 'topicElement', null);
    const stream = get(cells, 'sourceElement', null);
    const sink = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.STREAM_TOPIC_SINK,
      topic,
      stream,
      sink,
      firstLink,
      secondeLink,
    };
  }
  if (hasSource && !hasSink && !hasStream) {
    const topic = get(cells, 'targetElement', null);
    const source = get(cells, 'sourceElement', null);
    const link = get(cells, 'link');
    return { type: CONNECTION_TYPE.SOURCE_TOPIC, topic, source, link };
  }
  if (!hasSource && !hasSink && hasStream) {
    const link = get(cells, 'link');
    const source = cells.sourceElement;
    const target = cells.targetElement;
    if (source.kind === KIND.stream && target.kind === KIND.topic) {
      const topic = get(cells, 'targetElement', null);
      const stream = get(cells, 'sourceElement', null);
      return { type: CONNECTION_TYPE.STREAM_TOPIC, topic, stream, link };
    } else {
      const topic = get(cells, 'sourceElement', null);
      const stream = get(cells, 'targetElement', null);
      return { type: CONNECTION_TYPE.TOPIC_STREAM, topic, stream, link };
    }
  }
  if (!hasSource && hasSink && !hasStream) {
    const link = get(cells, 'link');
    const topic = get(cells, 'sourceElement', null);
    const sink = get(cells, 'targetElement', null);
    return { type: CONNECTION_TYPE.TOPIC_SINK, topic, sink, link };
  }
};
