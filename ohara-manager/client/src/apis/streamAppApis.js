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

import axiosInstance from './axios';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';
import { STREAM_APP_STATES, STREAM_APP_ACTIONS } from 'constants/pipelines';

export const fetchJars = async pipelineId => {
  try {
    const res = await axiosInstance.get(`/api/stream/jars/${pipelineId}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const uploadJar = async params => {
  try {
    const { pipelineId, file } = params;
    const url = `/api/stream/jars/${pipelineId}`;
    const formData = new FormData();
    formData.append('streamapp', file);
    const config = {
      headers: {
        'content-type': 'multipart/form-data',
      },
    };

    const res = await axiosInstance.post(url, formData, config);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteJar = async params => {
  try {
    const { id } = params;
    const url = `/api/stream/jars/${id}`;
    const res = await axiosInstance.delete(url);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateJarName = async params => {
  try {
    const { id, jarName } = params;
    const url = `/api/stream/jars/${id}`;
    const data = {
      jarName,
    };
    const res = await axiosInstance.put(url, data);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const fetchProperty = async id => {
  try {
    const res = await axiosInstance.get(`/api/stream/property/${id}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateProperty = async params => {
  try {
    const streamAppId = params.id;
    const url = `/api/stream/property/${streamAppId}`;
    const data = {
      name: params.name,
      fromTopics: params.fromTopics || [],
      toTopics: params.toTopics || [],
      instances: params.instances ? Number(params.instances) : 1,
    };
    const res = await axiosInstance.put(url, data);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

const mockStartOrStop = (id, action) => {
  return new Promise(resolve => {
    setTimeout(function() {
      resolve({
        data: {
          isSuccess: true,
          result: {
            id,
            state:
              action === STREAM_APP_ACTIONS.start
                ? STREAM_APP_STATES.running
                : '',
          },
        },
      });
    }, 1000);
  });
};

export const start = async id => {
  try {
    const res = await mockStartOrStop(id, STREAM_APP_ACTIONS.start);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const stop = async id => {
  try {
    const res = await mockStartOrStop(id, STREAM_APP_ACTIONS.stop);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

const mockDel = id => {
  return new Promise(resolve => {
    setTimeout(function() {
      resolve({
        data: {
          isSuccess: true,
        },
      });
    }, 1000);
  });
};

export const del = async id => {
  try {
    const res = await mockDel(id);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
