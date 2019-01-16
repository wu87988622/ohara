import { CONNECTOR_TYPES } from 'constants/pipelines';
import { createSource } from 'apis/pipelinesApis';

import { update } from '../pipelineToolbarUtils';

jest.mock('apis/pipelinesApis');

describe('update()', () => {
  it('should call updateGraph function if the given type is not exist in the current graph', async () => {
    const graph = [{ name: 'a', type: CONNECTOR_TYPES.topic }];
    const updateGraph = jest.fn();
    const connector = { className: CONNECTOR_TYPES.ftpSource };

    const res = { data: { result: { id: '1234' } } };

    createSource.mockImplementation(() => Promise.resolve(res));

    await update({ graph, updateGraph, connector });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith(
      {
        icon: 'fa-file-import',
        isActive: false,
        name: expect.any(String),
        to: '?',
        kind: CONNECTOR_TYPES.ftpSource,
        id: res.data.result.id,
      },
      CONNECTOR_TYPES.ftpSource,
    );
  });
});
