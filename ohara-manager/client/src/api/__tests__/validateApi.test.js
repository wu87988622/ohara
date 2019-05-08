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

import {
  validateNode,
  validateHdfs,
  validateFtp,
  validateRdb,
} from '../validateApi';
import { handleError, axiosInstance } from '../apiUtils';

jest.mock('../apiUtils');

const url = '/api/validate';

describe('validateNode()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    hostname: 'abc',
    port: 22,
    user: 'usr',
    password: 'ps',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateNode(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/node`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateNode(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/node`, params);
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

    await validateNode(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('validateHdfs()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    uri: '/path/to/wonderland',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateHdfs(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/hdfs`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateHdfs(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/hdfs`, params);
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

    await validateHdfs(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('validateRdb()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    a: 'a',
    b: 'b',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateRdb(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/rdb`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateRdb(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/rdb`, params);
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

    await validateRdb(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});

describe('validateFtp()', () => {
  afterEach(jest.clearAllMocks);

  const params = {
    a: 'a',
    b: 'b',
  };

  it('handles success http call', async () => {
    const res = {
      data: {
        isSuccess: true,
      },
    };

    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateFtp(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/ftp`, params);
    expect(result).toBe(res);
  });

  it('handles success http call but with server error', async () => {
    const res = {
      data: {
        isSuccess: false,
      },
    };
    axiosInstance.put.mockImplementation(() => Promise.resolve(res));

    const result = await validateFtp(params);

    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(axiosInstance.put).toHaveBeenCalledWith(`${url}/ftp`, params);
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

    await validateFtp(params);
    expect(axiosInstance.put).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledTimes(1);
    expect(handleError).toHaveBeenCalledWith(res);
  });
});
