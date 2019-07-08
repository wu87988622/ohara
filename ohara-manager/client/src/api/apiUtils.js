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

import axios from 'axios';
import toastr from 'toastr';
import { isString, get, has, isEmpty } from 'lodash';

export const handleError = err => {
  const message = get(err, 'data.errorMessage.message', null);

  if (isString(message)) {
    return toastr.error(message);
  }

  // Validate API returns an array which contains errors
  // of each node in it. That's why we're looping them over
  // and throwing error when `pass` is `false` here
  if (Array.isArray(message)) {
    const hasMessage = get(message, '[0][message]', null);

    if (hasMessage) {
      message.forEach(({ message, pass }) => {
        if (!pass) toastr.error(message);
      });
      return;
    }
  }

  const errorMessage = get(err, 'data.errorMessage', null);
  if (isString(errorMessage)) {
    return toastr.error(errorMessage);
  }

  const errorCount = get(err, 'errorCount', 0);

  if (errorCount) {
    const { settings } = err;

    const hasError = def => !isEmpty(def.value.errors);

    const errors = settings.filter(hasError).map(def => {
      return {
        fieldName: def.definition.displayName,
        errorMessage: def.value.errors.join(' '),
      };
    });

    // There could be multiple validation errors, so we need to loop thru them and
    // display respectively
    errors.forEach(error =>
      toastr.error(
        `<b>${error.fieldName.toUpperCase()}</b><br /> ${error.errorMessage}`,
      ),
    );

    return;
  }

  toastr.error(err || 'Internal Server Error');
};

const createAxios = () => {
  const instance = axios.create({
    validateStatus: status => status >= 200 && status < 300,
  });

  // Add a request interceptor
  instance.interceptors.request.use(config => {
    let headers = config.headers;
    if (
      config.method === 'post' ||
      config.method === 'put' ||
      config.method === 'patch'
    ) {
      if (headers['Content-Type'] === undefined) {
        headers = {
          ...headers,
          'Content-Type': 'application/json',
        };
      }
    }

    return {
      ...config,
      headers,
    };
  });

  // Add a response interceptor
  instance.interceptors.response.use(
    response => {
      if (response.config.url.includes('/validate')) {
        if (response.data.errorCount > 0) handleError(response.data);
      }

      return {
        data: {
          result: response.data,
          isSuccess: true,
        },
      };
    },
    error => {
      const { statusText, data: errorMessage } = error.response;
      return {
        data: {
          errorMessage: has(errorMessage, 'message')
            ? errorMessage
            : statusText,
          isSuccess: false,
        },
      };
    },
  );

  return instance;
};

export const axiosInstance = createAxios();
