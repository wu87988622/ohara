import * as clusterApis from 'apis/clusterApis';
import { fetchCluster } from '../pipelineUtils';

jest.mock('apis/clusterApis');

describe('fetchCluster()', () => {
  it('retruns the correct data with a successful call', async () => {
    const res = {
      data: {
        isSuccess: true,
        result: {
          sources: ['a', 'b'],
          sinks: ['c', 'd'],
          versionInfo: {
            version: 'abc',
            uesr: 'root',
            revision: 'abcdefghijk123',
            date: Date.now(),
          },
        },
      },
    };
    clusterApis.fetchCluster.mockImplementation(() => Promise.resolve(res));

    const { sources, sinks, versionInfo } = await fetchCluster();

    expect(clusterApis.fetchCluster).toHaveBeenCalledTimes(1);
    expect(sources).toEqual(res.data.result.sources);
    expect(sinks).toEqual(res.data.result.sinks);
    expect(versionInfo).toEqual(res.data.result.versionInfo);
  });
});
