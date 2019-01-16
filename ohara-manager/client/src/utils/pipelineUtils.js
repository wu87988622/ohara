import { CONNECTOR_TYPES } from 'constants/pipelines';
import * as _ from './commonUtils';

const getKeys = kind => {
  return Object.keys(CONNECTOR_TYPES).reduce((acc, iconKey) => {
    if (iconKey.includes(kind)) {
      acc.push(CONNECTOR_TYPES[iconKey]);
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

export const isTopic = kind => {
  return kind === CONNECTOR_TYPES.topic;
};

export const findByGraphId = (graph, id) => {
  const result = graph.find(x => x.id === id);
  return result;
};

export const updateTopic = (props, currTopic, connectorType) => {
  if (!currTopic || _.isEmpty(currTopic)) return;

  const { graph, match, updateGraph } = props;
  const connectorId = _.get(match, 'params.connectorId');

  let updateId = '';
  let update = null;

  if (connectorType === 'source') {
    const currConnector = findByGraphId(graph, connectorId);
    const to = _.isEmpty(currTopic) ? '?' : currTopic.id;
    update = { ...currConnector, to };
    updateId = currConnector.id;
  } else {
    const currTopicId = _.isEmpty(currTopic) ? '?' : currTopic.id;
    const topic = findByGraphId(graph, currTopicId);
    update = { ...topic, to: connectorId };
    updateId = currTopicId;
  }

  updateGraph(update, updateId);
};
