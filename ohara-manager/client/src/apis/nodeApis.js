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
import { toNumber } from 'lodash';
import * as _ from 'utils/commonUtils';
import { handleError } from 'utils/apiUtils';

export const fetchNodes = async () => {
  try {
    const res = await axiosInstance.get(`/api/nodes`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const createNode = async params => {
  try {
    const url = `/api/nodes`;
    const data = {
      name: params.name,
      port: toNumber(params.port),
      user: params.user,
      password: params.password,
    };

    const res = await axiosInstance.post(url, data);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const updateNode = async params => {
  try {
    const { name, port, user, password } = params;
    const url = `/api/nodes/${name}`;
    const data = {
      name,
      port: toNumber(port),
      user,
      password,
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
