import * as _ from 'utils/commonUtils';
import * as pipelinesApis from 'apis/pipelinesApis';
import { CONNECTOR_TYPES, ICON_MAPS } from 'constants/pipelines';

const isSource = kind => kind.includes('Source');
const isSink = kind => kind.includes('Sink');

/* eslint-disable array-callback-return */

const getNameByKind = kind => {
  if (isSource(kind)) {
    return 'Source';
  } else if (isSink(kind)) {
    return 'Sink';
  } else {
    return 'Topic';
  }
};

export const update = async ({ graph, updateGraph, connector }) => {
  let className = connector.className;
  className = className ? className : CONNECTOR_TYPES.topic;

  const connectorKind = getNameByKind(className);
  let connectorName = `Untitled ${connectorKind}`;

  // Default params for creating connectors
  const params = {
    name: connectorName,
    className: className,
    schema: [],
    topics: [],
    numberOfTasks: 1,
    configs: {},
  };

  let id;

  if (className === 'topic') {
    // Topic was created beforehand, it already has an ID.
    id = connector.id;
    connectorName = connector.name;
  } else if (isSource(className)) {
    const res = await pipelinesApis.createSource(params);
    id = _.get(res, 'data.result.id', null);
  } else if (isSink(className)) {
    const res = await pipelinesApis.createSink(params);
    id = _.get(res, 'data.result.id', null);
  }

  const update = {
    name: connectorName,
    kind: className,
    to: '?',
    isActive: false,
    icon: ICON_MAPS[className],
    id,
  };

  updateGraph(update, className);
};
