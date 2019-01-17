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

export const fetchStreamJars = async pipelineId => {
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

export const createStreamJar = async params => {
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

export const deleteStreamJar = async params => {
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

export const updateStreamJar = async params => {
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
