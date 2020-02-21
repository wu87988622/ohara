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
import { axiosInstance } from './utils/apiUtils';
import { sleep as sleepApi } from './utils/apiUtils';

const wait = async params => {
  const {
    url,
    checkFn,
    paramRes,
    retryCount = 0,
    maxRetry = 10,
    sleep = 2000,
  } = params;
  const res = await axiosInstance.get(url);
  if (get(res, 'data.isSuccess') && checkFn(res, paramRes) === true) {
    return res;
  }

  if (retryCount >= maxRetry) {
    return {
      data: {
        isSuccess: false,
        result: {},
      },
      errors: ['exceed max retry'],
    };
  }

  await sleepApi(sleep);
  return await wait({ ...params, retryCount: retryCount + 1 });
};
export default wait;
