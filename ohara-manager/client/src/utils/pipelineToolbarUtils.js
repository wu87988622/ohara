import * as _ from 'utils/commonUtils';
import * as pipelinesApis from 'apis/pipelinesApis';
import { CONNECTOR_TYPES, ICON_MAPS } from 'constants/pipelines';

const isSource = type => type.includes('Source');
const isSink = type => type.includes('Sink');

/* eslint-disable array-callback-return */
export const checkTypeExist = (type, graph) => {
  return graph.find(g => {
    const isExist =
      g.type === type ||
      (isSource(g.type) && isSource(type)) ||
      (isSink(g.type) && isSink(type));

    return isExist;
  });
};

const getNameByType = type => {
  if (isSource(type)) {
    return 'Source';
  } else if (isSink(type)) {
    return 'Sink';
  } else {
    return 'Topic';
  }
};

export const update = async ({ graph, updateGraph, connector }) => {
  let type = connector.className;
  type = type ? type : CONNECTOR_TYPES.topic;

  const connectorName = getNameByType(type);
  const result = checkTypeExist(type, graph);

  // Default params for creating connectors
  const params = {
    name: `Untitled ${connectorName}`,
    className: type,
    schema: [],
    topics: [],
    numberOfTasks: 1,
    configs: {},
  };

  let id;

  if (type === 'topic') {
    // Topic was created beforehand, it already has an ID.
    id = connector.id;
  } else if (isSource(type)) {
    const res = await pipelinesApis.createSource(params);
    id = _.get(res, 'data.result.id', null);
  } else if (isSink(type)) {
    const res = await pipelinesApis.createSink(params);
    id = _.get(res, 'data.result.id', null);
  }

  if (!_.isDefined(result)) {
    const update = {
      name: `Untitled ${connectorName}`,
      type,
      to: '?',
      isActive: false,
      icon: ICON_MAPS[type],
      id,
    };

    updateGraph(update, type);
  }
};
