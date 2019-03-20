/*
 * Copyright 2019 is-land
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import * as pipelineApi from '../pipelineApi';
import { handleError, axiosInstance } from 'utils/apiUtils';

jest.mock('utils/apiUtils');

const url = '/api/pipelines';

describe('fetchPipeline()', () => {
  afterEach(jest.clearAllMocks);

  const id = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.fetchPipeline(id);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/${id}`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.fetchPipeline(id);

    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/${id}`);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.reject(res));

    await pipelineApi.fetchPipeline(id);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('fetchPipelines()', () => {
  afterEach(jest.clearAllMocks);

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.fetchPipelines();
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(url);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.fetchPipelines();

    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(url);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.reject(res));

    await pipelineApi.fetchPipelines();
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('createPipeline()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    cluster: 'abc',
    a: 'a',
    b: 'b',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.createPipeline(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(url, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.createPipeline(params);

    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(url, params);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.reject(res));

    await pipelineApi.createPipeline(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('UpdatePipeline()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    id: 'abc',
    params: {
      a: 'a',
      b: 'b',
    },
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.updatePipeline(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(
      `${url}/${params.id}`,
      params.params,
    );
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.updatePipeline(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(
      `${url}/${params.id}`,
      params.params,
    );
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.reject(res));

    await pipelineApi.updatePipeline(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('deletePipeline()', () => {
  afterEach(jest.clearAllMocks);

  const id = '123';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.deletePipeline(id);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(`${url}/${id}`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.deletePipeline(id);

    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(`${url}/${id}`);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.delete.mockImplementation(() => Promise.reject(res));

    await pipelineApi.deletePipeline(id);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('queryRdb()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    a: 'a',
    b: 'b',
  };

  const url = '/api/query/rdb';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.queryRdb(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(`${url}`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await pipelineApi.queryRdb(params);

    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(`${url}`, params);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(result);
  });

  it('handles failed http call', async () => {
    const res = {
      data: {
        errorMessage: {
          message: 'error!',
        },
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.reject(res));

    await pipelineApi.queryRdb(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});
