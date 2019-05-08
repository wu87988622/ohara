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

import { fetchWorkers, fetchWorker, createWorker } from '../workerApi';
import { handleError, axiosInstance } from '../apiUtils';

jest.mock('../apiUtils');
const url = '/api/workers';

describe('fetchWorker()', () => {
  afterEach(jest.clearAllMocks);
  const workerName = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await fetchWorker(workerName);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/${workerName}`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await fetchWorker(workerName);

    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/${workerName}`);
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

    await fetchWorker(workerName);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('fetchWorkers()', () => {
  afterEach(jest.clearAllMocks);

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await fetchWorkers();
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

    const result = await fetchWorkers();

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

    await fetchWorkers();
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('createWorker()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    name: 'abc',
    clientPort: '22',
    nodeNames: ['a', 'b'],
    plugins: [],
  };

  const expectedParams = {
    name: params.name,
    clientPort: Number(params.clientPort),
    nodeNames: params.nodeNames,
    jars: params.plugins,
  };

  const config = { timeout: 180000 };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await createWorker(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(
      url,
      expectedParams,
      config,
    );
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await createWorker(params);

    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(
      url,
      expectedParams,
      config,
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

    axiosInstance.post.mockImplementation(() => Promise.reject(res));

    await createWorker(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});
