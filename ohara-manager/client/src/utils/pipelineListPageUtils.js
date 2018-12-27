export const addPipelineStatus = (pipelines = []) => {
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
  const { id: pipelineId } = pipeline;
  return `${match.url}/edit/${pipelineId}`;
};
