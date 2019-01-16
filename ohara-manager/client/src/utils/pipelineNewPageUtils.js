import { isSource, isSink, isTopic } from './pipelineUtils';

export const getConnectors = connectors => {
  const sources = connectors
    .filter(({ kind }) => {
      return isSource(kind);
    })
    .map(({ id }) => id);

  const sinks = connectors
    .filter(({ kind }) => {
      return isSink(kind);
    })
    .map(({ id }) => id);

  const topics = connectors.filter(({ kind }) => {
    return isTopic(kind);
  });

  return { sources, sinks, topics };
};

export const addPipelineStatus = pipeline => {
  const status = pipeline.objects.filter(p => p.state === 'RUNNING');
  const _status = status.length >= 2 ? 'Running' : 'Stopped';

  return {
    ...pipeline,
    status: _status,
  };
};

export const updatePipelineParams = (pipelines, update) => {
  const { name, rules } = pipelines;

  let params;
  let updateRule;

  if (update && update.id) {
    const { id, kind, to } = update;

    if (to === '?') {
      updateRule = isSink(kind) ? { '?': id } : { [id]: '?' };
    } else if (isSink(kind)) {
      const connector = Object.values(rules).find(x => x === id);
      updateRule = { [to]: connector };
    } else {
      const connector = Object.keys(rules).find(x => x === id);
      updateRule = { [connector]: to };
    }

    params = {
      name,
      rules: { ...rules, ...updateRule },
    };
  } else {
    params = { name, rules };
  }

  return params;
};

export const updateGraph = (graph, update, id) => {
  const idx = graph.findIndex(g => g.id === id);
  let updatedGraph = [];

  if (idx === -1) {
    updatedGraph = [...graph, update];
  } else {
    updatedGraph = [
      ...graph.slice(0, idx),
      { ...graph[idx], ...update },
      ...graph.slice(idx + 1),
    ];
  }

  return updatedGraph;
};

export const loadGraph = pipelines => {
  const { objects, rules } = pipelines;

  const updatedGraph = Object.keys(rules).map(x => {
    if (x === '?') {
      const target = objects.find(object => object.id === rules[x]);

      return {
        ...target,
        to: '?',
      };
    }

    const target = objects.find(object => object.id === x);

    if (rules[x] !== '?') {
      return {
        ...target,
        to: rules[x],
      };
    }

    return {
      ...target,
      to: '?',
    };
  });

  return updatedGraph;
};
