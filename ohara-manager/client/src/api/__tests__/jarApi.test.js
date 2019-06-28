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

import { fetchJars, deleteJar, createJar } from '../jarApi';
import { handleError, axiosInstance } from '../apiUtils';

jest.mock('../apiUtils');

const url = '/api/jars?group=wk';

describe('fetchJars()', () => {
  afterEach(jest.clearAllMocks);

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await fetchJars('wk');
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

    const result = await fetchJars('wk');

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

    await fetchJars();
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('createJar()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    file: {},
    workerClusterName: 'wk01',
  };

  const formData = new FormData();
  formData.append('jar', params.file);
  formData.append('group', params.workerClusterName);

  const config = {
    headers: {
      'content-type': 'multipart/form-data',
    },
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await createJar(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(
      '/api/jars',
      formData,
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

    const result = await createJar(params);

    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(
      '/api/jars',
      formData,
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

    await createJar(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('deleteJar()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    name: '1234',
    workerClusterName: 'wk00',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await deleteJar(params);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(
      `/api/jars/${params.name}?group=${params.workerClusterName}`,
    );
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await deleteJar(params);

    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(
      `/api/jars/${params.name}?group=${params.workerClusterName}`,
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

    axiosInstance.delete.mockImplementation(() => Promise.reject(res));

    await deleteJar(params);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});
