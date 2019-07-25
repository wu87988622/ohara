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
import { isString, get, has, isEmpty, isUndefined } from 'lodash';

export const handleError = error => {
  if (isUndefined(error)) return; // prevent `undefined` error from throwing `Internal Server Error`

  const message = get(error, 'data.errorMessage.message', null);
  if (isString(message)) {
    return message;
  }

  const errorMessage = get(error, 'data.errorMessage', null);
  if (isString(errorMessage)) {
    return errorMessage;
  }

  return error.message || 'Internal Server Error';
};

export const handleConnectorValidationError = res => {
  const error = res.data.result;
  if (error.errorCount === 0) return;
  const { settings } = error;

  const hasError = def => !isEmpty(def.value.errors);
  const errors = settings.filter(hasError).map(def => {
    return {
      fieldName: def.definition.displayName,
      errorMessage: def.value.errors.join(' '),
    };
  });

  // There could be multiple validation errors, so we need to loop thru them and
  // display respectively
  return errors.map(error => {
    return `<b>${error.fieldName.toUpperCase()}</b><br /> ${
      error.errorMessage
    }`;
  });
};

export const handleNodeValidationError = res => {
  const error = res.data.result;
  if (!Array.isArray(error)) return;
  return error.map(node => {
    if (!node.pass) {
      return node.message;
    } else {
      return '';
    }
  });
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
