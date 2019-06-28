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

import * as streamApi from '../streamApi';
import { handleError, axiosInstance } from '../apiUtils';

jest.mock('../apiUtils');
const url = '/api/stream';

describe('fetchProperty()', () => {
  afterEach(jest.clearAllMocks);

  const id = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.fetchProperty(id);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/property/${id}`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.get.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.fetchProperty(id);

    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(axiosInstance.get).toHaveBeenCalledWith(`${url}/property/${id}`);
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

    await streamApi.fetchProperty(id);
    expect(axiosInstance.get).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('createProperty()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    name: 'stream',
    jar: {
      group: 'wk00',
      name: 'streamjar',
    },
  };

  it.only('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.createProperty(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(`${url}/property`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.post.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.updateProperty(params);

    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(axiosInstance.post).toHaveBeenCalledWith(`${url}/property`, params);
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

    await streamApi.updateProperty(params);
    expect(axiosInstance.post).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('updateProperty()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    id: 'abc',
    streamAppId: '123',
    name: 'name',
    formTopics: ['f'],
    toTopics: ['t'],
    instances: 1,
  };

  const streamAppId = params.id;
  const data = {
    name: params.name,
    from: params.from || [],
    to: params.to || [],
    instances: params.instances ? Number(params.instances) : 1,
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.updateProperty(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(
      `${url}/property/${streamAppId}`,
      data,
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

    const result = await streamApi.updateProperty(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(
      `${url}/property/${streamAppId}`,
      data,
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

    await streamApi.updateProperty(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('deleteProperty()', () => {
  afterEach(jest.clearAllMocks);

  const id = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.deleteProperty(id);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(`${url}/property/${id}`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.delete.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.deleteProperty(id);

    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(axiosInstance.delete).toHaveBeenCalledWith(`${url}/property/${id}`);
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

    await streamApi.deleteProperty(id);
    expect(axiosInstance.delete).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('startStreamApp()', () => {
  afterEach(jest.clearAllMocks);

  const id = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.startStreamApp(id);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/${id}/start`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.startStreamApp(id);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/${id}/start`);
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

    await streamApi.startStreamApp(id);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('stopStreamApp()', () => {
  afterEach(jest.clearAllMocks);

  const id = 'abc';

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.stopStreamApp(id);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/${id}/stop`);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await streamApi.stopStreamApp(id);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/${id}/stop`);
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

    await streamApi.stopStreamApp(id);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});
