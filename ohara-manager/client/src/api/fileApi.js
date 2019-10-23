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

import { axiosInstance } from './apiUtils';

export const fetchFiles = async group => {
  try {
    const url = group ? `/api/files?group=${group}` : `/api/files`;
    const res = await axiosInstance.get(url);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) throw new Error(res.errorMessage);

    return res;
  } catch (error) {
    throw new Error(error);
  }
};

export const fetchFile = async params => {
  try {
    const res = await axiosInstance.get(
      `/api/files/${params.name}?group=${params.group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) throw new Error(res.errorMessage);

    return res;
  } catch (error) {
    throw new Error(error);
  }
};

export const deleteFile = async params => {
  try {
    const res = await axiosInstance.delete(
      `/api/files/${params.name}?group=${params.group}`,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) throw new Error(res.errorMessage);

    return res;
  } catch (error) {
    throw new Error(error);
  }
};

export const updateFile = async params => {
  try {
    const data = {
      tags: params.tags,
    };
    const res = await axiosInstance.put(
      `/api/files/${params.name}?group=${params.group}`,
      data,
    );
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) throw new Error(res.errorMessage);

    return res;
  } catch (error) {
    throw new Error(error);
  }
};

export const uploadFile = async params => {
  try {
    const url = `/api/files`;
    let formData = new FormData();
    formData.append('file', params.file);
    formData.append('group', params.group);
    formData.append('tags', JSON.stringify(params.tags));

    const config = {
      headers: {
        'content-type': 'multipart/form-data',
      },
    };
    const res = await axiosInstance.post(url, formData, config);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) throw new Error(res.errorMessage);

    return res;
  } catch (error) {
    throw new Error(error);
  }
};
