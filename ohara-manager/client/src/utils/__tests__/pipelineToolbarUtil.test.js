import { ICON_KEYS } from 'constants/pipelines';

import { checkTypeExist, update } from '../pipelineToolbarUtil';

describe('checkTypeExist()', () => {
  it('should get the item that matches the same type', () => {
    const type = ICON_KEYS.ftpSink;
    const graph = [
      { name: 'a', type: ICON_KEYS.ftpSink },
      { name: 'b', type: ICON_KEYS.ftpSource },
    ];
    expect(checkTypeExist(type, graph)).toBe(graph[0]);
  });

  it('should return undefined if no match has found', () => {
    const type = ICON_KEYS.ftpSink;
    const graph = [
      { name: 'a', type: ICON_KEYS.ftpSource },
      { name: 'b', type: ICON_KEYS.jdbcSource },
    ];

    expect(checkTypeExist(type, graph)).toBeUndefined();
  });
});

describe('update()', () => {
  it('should call updateGraph function if the given type is not exist in the current graph', () => {
    const graph = [{ name: 'a', type: ICON_KEYS.topic }];
    const updateGraph = jest.fn();
    const evtObj = {
      target: {
        dataset: { id: ICON_KEYS.ftpSource },
      },
    };

    update({ graph, updateGraph, evtObj });

    expect(updateGraph).toHaveBeenCalledTimes(1);
    expect(updateGraph).toHaveBeenCalledWith(
      {
        icon: 'fa-upload',
        id: expect.any(String),
        isActive: false,
        name: expect.any(String),
        to: '?',
        type: ICON_KEYS.ftpSource,
      },
      ICON_KEYS.ftpSource,
    );
  });

  it('should not call updateGraph function if the given type is included in the current graph', () => {
    const graph = [{ name: 'a', type: ICON_KEYS.topic }];
    const updateGraph = jest.fn();
    const evtObj = {
      target: {
        dataset: { id: ICON_KEYS.topic },
      },
    };

    update({ graph, updateGraph, evtObj });
    expect(updateGraph).toHaveBeenCalledTimes(0);
  });
});
