import { v4 as uuid4 } from 'uuid';

import * as _ from 'utils/commonUtils';
import * as clusterApis from 'apis/clusterApis';
import * as PIPELINES from 'constants/pipelines';

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

export const update = ({ graph, updateGraph, connector }) => {
  let type = connector.className;

  // TODO: replace the svg icon with the HTML one and so we'll get the target.dataset.id back
  type = type ? type : PIPELINES.CONNECTOR_KEYS.hdfsSink;

  const result = checkTypeExist(type, graph);

  if (!_.isDefined(result)) {
    const update = {
      name: `Untitled ${isSource(type) ? 'source' : 'sink'}`,
      type,
      to: '?',
      isActive: false,
      icon: PIPELINES.ICON_MAPS[type],
      localId: uuid4(),
    };

    updateGraph(update, type);
  }
};

export const fetchCluster = async () => {
  const res = await clusterApis.fetchCluster();

  const isSuccess = _.get(res, 'data.isSuccess');
  if (isSuccess) {
    const { sources, sinks } = res.data.result;
    return { sources, sinks };
  }
};
