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

import { get } from 'lodash';

import { handleError, axiosInstance } from 'utils/apiUtils';

export const fetchHdfs = async () => {
  try {
    const res = await axiosInstance.get('/api/hdfs');
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateHdfs = async ({ uri }) => {
  try {
    const res = await axiosInstance.put('/api/validate/hdfs', {
      uri,
    });
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateHdfs = async params => {
  try {
    const res = await axiosInstance.put(`/api/hdfs/${params.id}`, params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveHdfs = async ({ name, uri }) => {
  try {
    const res = await axiosInstance.post('/api/hdfs', {
      name,
      uri,
    });
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteHdfs = async id => {
  try {
    const res = await axiosInstance.delete(`/api/hdfs/${id}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchJdbc = async () => {
  try {
    const res = await axiosInstance.get('/api/jdbc');
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateRdb = async params => {
  try {
    const res = await axiosInstance.put('/api/validate/rdb', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateJdbc = async params => {
  try {
    const res = await axiosInstance.put(`/api/jdbc/${params.id}`, params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveJdbc = async params => {
  try {
    const res = await axiosInstance.post('/api/jdbc', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteJdbc = async id => {
  try {
    const res = await axiosInstance.delete(`/api/jdbc/${id}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchFtp = async () => {
  try {
    const res = await axiosInstance.get('/api/ftp');
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateFtp = async params => {
  try {
    const res = await axiosInstance.put('/api/validate/ftp', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateFtp = async params => {
  try {
    const res = await axiosInstance.put(`/api/ftp/${params.id}`, params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveFtp = async params => {
  try {
    const res = await axiosInstance.post('/api/ftp', params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteFtp = async id => {
  try {
    const res = await axiosInstance.delete(`/api/ftp/${id}`);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
