import { CONNECTOR_KEYS } from 'constants/pipelines';

import { checkTypeExist, update } from '../pipelineToolbarUtils';

jest.mock('apis/clusterApis');

describe('checkTypeExist()', () => {
  it('should get the item that matches the same type', () => {
    const type = CONNECTOR_KEYS.ftpSink;
    const graph = [
      { name: 'a', type: CONNECTOR_KEYS.ftpSink },
      { name: 'b', type: CONNECTOR_KEYS.ftpSource },
    ];
    expect(checkTypeExist(type, graph)).toBe(graph[0]);
  });

  it('should return undefined if no match has found', () => {
    const type = CONNECTOR_KEYS.ftpSink;
    const graph = [
      { name: 'a', type: CONNECTOR_KEYS.ftpSource },
      { name: 'b', type: CONNECTOR_KEYS.jdbcSource },
    ];

    expect(checkTypeExist(type, graph)).toBeUndefined();
  });
});

describe('update()', () => {
  it('should call updateGraph function if the given type is not exist in the current graph', () => {
    const graph = [{ name: 'a', type: CONNECTOR_KEYS.topic }];
    const updateGraph = jest.fn();
    const connector = { className: CONNECTOR_KEYS.ftpSource };

    update({ graph, updateGraph, connector });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith(
      {
        icon: 'fa-file-import',
        localId: expect.any(String),
        isActive: false,
        name: expect.any(String),
        to: '?',
        type: CONNECTOR_KEYS.ftpSource,
      },
      CONNECTOR_KEYS.ftpSource,
    );
  });

  it('should not call updateGraph function if the given type is included in the current graph', () => {
    const graph = [{ name: 'a', type: CONNECTOR_KEYS.topic }];
    const updateGraph = jest.fn();
    const connector = { className: CONNECTOR_KEYS.topic };

    update({ graph, updateGraph, connector });
    expect(updateGraph).toHaveBeenCalledTimes(0);
  });
});
