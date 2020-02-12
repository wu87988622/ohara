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
  STREAM_TOPIC_STREAM: 'stream_topic_stream',
  SOURCE_TOPIC: 'source_topic',
  STREAM_TOPIC: 'stream_topic',
  TOPIC_SINK: 'topic_sink',
  TOPIC_STREAM: 'topic_stream',
};

export const getCellState = cell => {
  return get(cell, 'data.state', CELL_STATUS.stopped).toLowerCase();
};

export const getConnectionOrder = cells => {
  const hasSource = get(cells, 'sourceElement.kind', null) === KIND.source;
  const hasSink = get(cells, 'targetElement.kind', null) === KIND.sink;
  const hasFromStream = get(cells, 'targetElement.kind', null) === KIND.stream;
  const hasToStream = get(cells, 'sourceElement.kind', null) === KIND.stream;

  if (hasSource && hasSink && !hasFromStream && !hasToStream) {
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
  if (hasSource && !hasSink && hasFromStream && !hasToStream) {
    const topic = get(cells, 'topicElement', null);
    const source = get(cells, 'sourceElement', null);
    const fromStream = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.SOURCE_TOPIC_STREAM,
      topic,
      source,
      fromStream,
      firstLink,
      secondeLink,
    };
  }
  if (!hasSource && hasSink && hasToStream && !hasFromStream) {
    const topic = get(cells, 'topicElement', null);
    const toStream = get(cells, 'sourceElement', null);
    const sink = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.STREAM_TOPIC_SINK,
      topic,
      toStream,
      sink,
      firstLink,
      secondeLink,
    };
  }
  if (!hasSource && !hasSink && hasToStream && hasFromStream) {
    const topic = get(cells, 'topicElement', null);
    const toStream = get(cells, 'sourceElement', null);
    const fromStream = get(cells, 'targetElement', null);
    const firstLink = get(cells, 'firstLink', null);
    const secondeLink = get(cells, 'secondeLink', null);
    return {
      type: CONNECTION_TYPE.STREAM_TOPIC_STREAM,
      topic,
      toStream,
      fromStream,
      firstLink,
      secondeLink,
    };
  }
  if (hasSource && !hasSink && !hasToStream && !hasFromStream) {
    const topic = get(cells, 'targetElement', null);
    const source = get(cells, 'sourceElement', null);
    const link = get(cells, 'link');
    return { type: CONNECTION_TYPE.SOURCE_TOPIC, topic, source, link };
  }
  if (!hasSource && !hasSink && !hasToStream && !hasFromStream) {
    const topic = get(cells, 'targetElement', null);
    const source = get(cells, 'sourceElement', null);
    const link = get(cells, 'link');
    return { type: CONNECTION_TYPE.SOURCE_TOPIC, topic, source, link };
  }
  if (!hasSource && !hasSink && hasToStream && !hasFromStream) {
    const link = get(cells, 'link');
    const topic = get(cells, 'targetElement', null);
    const toStream = get(cells, 'sourceElement', null);
    return { type: CONNECTION_TYPE.STREAM_TOPIC, topic, toStream, link };
  }
  if (!hasSource && !hasSink && !hasToStream && hasFromStream) {
    const link = get(cells, 'link');
    const topic = get(cells, 'sourceElement', null);
    const fromStream = get(cells, 'targetElement', null);
    return { type: CONNECTION_TYPE.TOPIC_STREAM, topic, fromStream, link };
  }
  if (!hasSource && hasSink && !hasToStream && !hasFromStream) {
    const link = get(cells, 'link');
    const topic = get(cells, 'sourceElement', null);
    const sink = get(cells, 'targetElement', null);
    return { type: CONNECTION_TYPE.TOPIC_SINK, topic, sink, link };
  }
};
