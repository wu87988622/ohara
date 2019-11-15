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

/* eslint-disable no-console */
import {
  option as isOption,
  object as isObject,
  getType,
  getGenerate,
} from './validation';
import axios from 'axios';
import { get, has, isEmpty, isUndefined, isArray, isString } from 'lodash';

const converterUtil = (params, api) => {
  const converterKeys = Object.keys(api.reqConverter ? api.reqConverter : {});
  const paramsKeys = Object.keys(params);
  if (converterKeys.length === 0) return params;

  converterKeys.forEach(cKey =>
    paramsKeys
      .filter(pKey => cKey === pKey)
      .map(pKey => (params[api.reqConverter[cKey]] = params[pKey]))
      .forEach(pKey => {
        delete params[pKey];
        console.warn(
          pKey +
            ' has been changed ' +
            api.reqConverter[cKey] +
            ' please modify params!',
        );
      }),
  );
  return params;
};

const generateUtil = (params = {}, api) => {
  const requestBody = {};
  const requestKeys = Object.keys(api);
  const paramsKeys = Object.keys(params);
  requestKeys
    .map(rKey => {
      if (isObject(api[rKey])) {
        const objs = generateUtil(params[rKey], api[rKey]);
        Object.keys(objs).forEach(gKey => {
          const obj = {};
          obj[gKey] = objs[gKey];
          requestBody[rKey] = obj;
        });
        return null;
      } else {
        return rKey;
      }
    })
    .filter(Boolean)
    .filter(rKey => getGenerate(api[rKey]))
    .filter(rKey => !paramsKeys.includes(rKey))
    .forEach(rKey => {
      const generate = getGenerate(api[rKey]);
      requestBody[rKey] = generate(api[rKey]);
    });
  return requestBody;
};

const optionUtil = (params = {}, api, requestBody = {}, parentKey) => {
  const requestKeys = Object.keys(api);
  const paramsKeys = Object.keys(params);
  const requestBodyKeys = Object.keys(requestBody);
  requestKeys
    .map(rKey => {
      if (isObject(api[rKey])) {
        optionUtil(params[rKey], api[rKey], requestBody[rKey], rKey);
        return null;
      } else {
        return rKey;
      }
    })
    .filter(Boolean)
    .forEach(rKey => {
      if (
        !paramsKeys.includes(rKey) &&
        !requestBodyKeys.includes(rKey) &&
        !api[rKey].includes(isOption)
      ) {
        const errorKey = parentKey ? `${parentKey}.${rKey}` : rKey;
        console.error(errorKey + ' is not the option!');
      }
    });
};

const typeUtil = (params, api, requestBody, parentKey) => {
  const requestKeys = Object.keys(api);
  const paramsKeys = Object.keys(params);

  requestKeys.forEach(rKey =>
    paramsKeys
      .filter(pKey => rKey === pKey)
      .map(pKey => {
        if (isObject(api[rKey])) {
          const obj = typeUtil(params[pKey], api[rKey], {}, parentKey);
          requestBody[pKey] = Object.assign(obj, requestBody[pKey]);
          return null;
        } else {
          return pKey;
        }
      })
      .filter(Boolean)
      .forEach(pKey => {
        if (getType(api[rKey])(params[pKey])) {
          requestBody[rKey] = params[pKey];
        } else {
          const errorKey = parentKey ? `${parentKey}.${rKey}` : rKey;
          console.error(errorKey + ' type is wrong!');
        }
      }),
  );
  return requestBody;
};

const getNestObjectByString = (resKey, params) => {
  let obj = params;
  const splitKeys = resKey.split('.');
  splitKeys.forEach(sKey => {
    if (obj[sKey]) {
      obj = obj[sKey];
    }
  });
  return obj;
};

export const requestUtil = (params, api, definitionsBody) => {
  let requestBody = {};
  const converterParams = converterUtil(params, api);
  requestBody = generateUtil(converterParams, api.request(definitionsBody));
  optionUtil(params, api.request(definitionsBody), requestBody);
  requestBody = typeUtil(params, api.request(definitionsBody), requestBody);
  return requestBody;
};

export const responseUtil = (params, api) => {
  const { result, isSuccess } = params.data;
  if (!isSuccess) return;
  const warns = new Set();
  let resList = isArray(result) ? result : [result];
  resList.forEach((res, i) => {
    const paramsKeys = Object.keys(res);
    const resConverterKeys = Object.keys(
      api.resConverter ? api.resConverter : {},
    );
    resConverterKeys.forEach(resKey => {
      paramsKeys.forEach(pKey => {
        if (resKey.indexOf(pKey) > -1) {
          res[api.resConverter[resKey]] = getNestObjectByString(resKey, res);
          resList[i] = res;
          warns.add(
            `${pKey} has been changed ${api.resConverter[resKey]} please modify params!`,
          );
        }
      });
    });
  });
  warns.forEach(warn => console.warn(warn));
  return isArray(result) ? resList : resList[0];
};

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

const regularObject = (objs, originKey, changeKey) => {
  const newObjs = isArray(objs) ? objs : [objs];
  newObjs.forEach((newObj, i) => {
    Object.keys(newObj).forEach(key => {
      if (isObject(newObjs[i][key])) {
        const obj = regularObject(newObjs[i][key], originKey, changeKey);
        newObjs[i][key] = obj;
      } else if (key.indexOf(originKey) !== -1) {
        const newKey = key.split(originKey).join(changeKey);
        newObjs[i][newKey] = newObjs[i][key];
        delete newObjs[i][key];
      }
    });
  });
  return isArray(objs) ? newObjs : newObjs[0];
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
      if (config.data) {
        config.data = regularObject(config.data, '__', '.');
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
      if (response.data) {
        response.data = regularObject(response.data, '.', '__');
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

export const sleep = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};
