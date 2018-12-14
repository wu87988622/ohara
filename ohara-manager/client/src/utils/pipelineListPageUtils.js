import * as _ from './commonUtils';
import { isSource, isSink } from './pipelineUtils';

export const addPipelineStatus = pipelines => {
  const _pipelines = pipelines.reduce((acc, pipeline) => {
    const status = pipeline.objects.filter(p => p.state === 'RUNNING');
    const _status = status.length >= 2 ? 'Running' : 'Stopped';

    return [
      ...acc,
      {
        ...pipeline,
        status: _status,
      },
    ];
  }, []);

  return _pipelines;
};

export const getEditUrl = (pipeline, match) => {
  const { uuid: pipelineId, objects: connectors } = pipeline;

  const source = connectors.reduce((acc, connector) => {
    if (isSource(connector.kind)) {
      acc += connector.kind;
    }
    return acc;
  }, '');

  const { topic: topicId, source: sourceId, sink: sinkId } = connectors.reduce(
    (acc, { uuid, kind }) => {
      if (kind === 'topic') {
        acc[kind] = uuid;
      }

      if (isSource(kind)) {
        acc['source'] = uuid;
      }

      if (isSink(kind)) {
        acc['sink'] = uuid;
      }

      return acc;
    },
    {},
  );

  const pageName = _.isEmptyStr(source) ? 'topic' : source;

  const baseUrl = `${match.url}/edit/${pageName}/${pipelineId}/${topicId}`;
  let url = baseUrl;

  if (sinkId) {
    url = `${baseUrl}/${sourceId}/${sinkId}`;
  } else if (sourceId) {
    url = `${baseUrl}/${sourceId}/`;
  }

  return url;
};
