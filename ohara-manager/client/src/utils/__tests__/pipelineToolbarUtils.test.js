import { CONNECTOR_TYPES } from 'constants/pipelines';
import { createSource } from 'apis/pipelinesApis';

import { checkTypeExist, update } from '../pipelineToolbarUtils';

jest.mock('apis/pipelinesApis');

describe('checkTypeExist()', () => {
  it('should get the item that matches the same type', () => {
    const type = CONNECTOR_TYPES.ftpSink;
    const graph = [
      { name: 'a', type: CONNECTOR_TYPES.ftpSink },
      { name: 'b', type: CONNECTOR_TYPES.ftpSource },
    ];
    expect(checkTypeExist(type, graph)).toBe(graph[0]);
  });

  it('should return undefined if no match has found', () => {
    const type = CONNECTOR_TYPES.ftpSink;
    const graph = [
      { name: 'a', type: CONNECTOR_TYPES.ftpSource },
      { name: 'b', type: CONNECTOR_TYPES.jdbcSource },
    ];

    expect(checkTypeExist(type, graph)).toBeUndefined();
  });
});

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
        type: CONNECTOR_TYPES.ftpSource,
        id: res.data.result.id,
      },
      CONNECTOR_TYPES.ftpSource,
    );
  });

  it('should not call updateGraph function if the given type is included in the current graph', () => {
    const graph = [{ name: 'a', type: CONNECTOR_TYPES.topic }];
    const updateGraph = jest.fn();
    const connector = { className: CONNECTOR_TYPES.topic };

    update({ graph, updateGraph, connector });
    expect(updateGraph).toHaveBeenCalledTimes(0);
  });
});
