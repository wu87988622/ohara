import * as _ from './commonUtils';
import * as pipelinesApis from 'apis/pipelinesApis';
import * as topicApis from 'apis/topicApis';

import { CONNECTOR_KEYS } from 'constants/pipelines';

const getKeys = kind => {
  return Object.keys(CONNECTOR_KEYS).reduce((acc, iconKey) => {
    if (iconKey.includes(kind)) {
      acc.push(CONNECTOR_KEYS[iconKey]);
    }

    return acc;
  }, []);
};

const sourceKeys = getKeys('Source');
const sinkKeys = getKeys('Sink');

export const isSource = kind => {
  return sourceKeys.some(sourceKey => kind.includes(sourceKey));
};

export const isSink = kind => {
  return sinkKeys.some(sinkKey => kind.includes(sinkKey));
};

export const fetchPipelines = async () => {
  const res = await pipelinesApis.fetchPipelines();
  const pipelines = _.get(res, 'data.result', null);

  if (pipelines) return pipelines;
};

export const fetchTopics = async () => {
  const res = await topicApis.fetchTopics();
  const topics = _.get(res, 'data.result', null);

  if (topics) {
    return topics;
  }

  return null;
};

export const fetchSink = async uuid => {
  if (!uuid) return;

  const res = await pipelinesApis.fetchSink(uuid);
  const sink = _.get(res, 'data.result', null);
  if (sink) {
    return sink;
  }

  return null;
};

export const fetchSource = async uuid => {
  if (!uuid) return;

  const res = await pipelinesApis.fetchSource(uuid);
  const source = _.get(res, 'data.result', null);

  if (source) {
    return source;
  }

  return null;
};
