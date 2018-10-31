import { v4 as uuid4 } from 'uuid';

import * as _ from 'utils/helpers';
import { ICON_KEYS, ICON_MAPS } from 'constants/pipelines';

/* eslint-disable array-callback-return */
export const checkTypeExist = (type, graph) => {
  return graph.find(g => {
    const isExist =
      g.type === type ||
      (g.type.includes('Source') && type.includes('Source')) ||
      (g.type.includes('Sink') && type.includes('Sink'));

    if (isExist) {
      return g;
    }
  });
};

export const update = ({ graph, updateGraph, evtObj }) => {
  let type = _.get(evtObj, 'target.dataset.id', null);

  // TODO: replace the svg icon with the HTML one and so we'll get the target.dataset.id back
  type = type ? type : ICON_KEYS.hdfsSink;

  const isTypeExist = checkTypeExist(type, graph);

  if (_.isEmpty(isTypeExist)) {
    const update = {
      name: `Untitled ${type}`,
      type,
      to: '?',
      isActive: false,
      icon: ICON_MAPS[type],
      id: uuid4(),
    };

    updateGraph(update, type);
  }
};
