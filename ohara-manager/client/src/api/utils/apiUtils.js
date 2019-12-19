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
import { has, isEmpty, isArray } from 'lodash';

export const getKey = params => {
  const name = params.name || params.hostname || undefined;
  const group = params.group || undefined;
  return JSON.stringify({ group, name });
};

const converterUtil = (warnReasons, params, api) => {
  const converterKeys = Object.keys(api.reqConverter ? api.reqConverter : {});
  const paramsKeys = Object.keys(params);
  if (converterKeys.length === 0) return params;

  converterKeys.forEach(cKey =>
    paramsKeys
      .filter(pKey => cKey === pKey)
      .map(pKey => (params[api.reqConverter[cKey]] = params[pKey]))
      .forEach(pKey => {
        delete params[pKey];
        const warnData = {
          message:
            pKey +
            ' has been changed ' +
            api.reqConverter[cKey] +
            ' please modify params!',
        };
        warnReasons.push(warnData);
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

const optionUtil = (warnReasons, data, parentKey) => {
  const { params = {}, api, body = {} } = data;
  const requestKeys = Object.keys(api);
  const paramsKeys = Object.keys(params);
  const bodyKeys = Object.keys(body);
  requestKeys
    .map(rKey => {
      if (isObject(api[rKey])) {
        optionUtil(
          warnReasons,
          {
            params: params[rKey],
            api: api[rKey],
            body: body[rKey],
          },
          rKey,
        );
        return null;
      } else {
        return rKey;
      }
    })
    .filter(Boolean)
    .forEach(rKey => {
      if (
        !paramsKeys.includes(rKey) &&
        !bodyKeys.includes(rKey) &&
        !api[rKey].includes(isOption)
      ) {
        const errorKey = parentKey ? `${parentKey}.${rKey}` : rKey;
        const warnData = {
          message: errorKey + ' is not defined as optional.',
        };
        warnReasons.push(warnData);
      }
    });
};

export const typeUtil = (warnReasons, data, parentKey = undefined) => {
  const { params, api, body, definitions } = data;
  const apiKeys = Object.keys(api);
  const paramsKeys = Object.keys(params);

  apiKeys.forEach(rKey =>
    paramsKeys
      .filter(pKey => rKey === pKey)
      .map(pKey => {
        if (isObject(api[rKey])) {
          const obj = typeUtil(
            warnReasons,
            {
              params: params[pKey],
              api: api[rKey],
              body: {},
              definitions,
            },
            pKey,
          );
          body[pKey] = Object.assign(obj, body[pKey]);
          return null;
        } else {
          return pKey;
        }
      })
      .filter(Boolean)
      .forEach(pKey => {
        if (getType(api[rKey])(params[pKey])) {
          body[rKey] = params[pKey];
        } else {
          const errorKey = parentKey ? `${parentKey}.${rKey}` : rKey;
          const warnData = {
            message:
              `the field '${errorKey}' type is wrong. ` +
              `expected: '${getType(api[rKey]).name}', ` +
              `actual value is: '${JSON.stringify(params[pKey])}'`,
            stack:
              definitions && definitions[errorKey]
                ? // show the definitions as detail message
                  definitions[errorKey]
                : // if no definition, use undefined instead
                  undefined,
          };
          warnReasons.push(warnData);
        }
      }),
  );
  return body;
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
  let warnReasons = [];
  let requestBody = {};
  const converterParams = converterUtil(warnReasons, params, api);
  requestBody = generateUtil(converterParams, api.request(definitionsBody));
  optionUtil(warnReasons, {
    params,
    api: api.request(definitionsBody),
    body: requestBody,
  });
  const data = {
    params,
    api: api.request(definitionsBody),
    body: requestBody,
    definitions: api.definitions,
  };
  requestBody = typeUtil(warnReasons, data);
  if (!isEmpty(warnReasons)) {
    // since request does not "response" anything
    // we need to print the warnings to console
    warnReasons.forEach(warn => console.log(warn));
  }
  return requestBody;
};

export const responseUtil = (params, api) => {
  let finalRes = {
    status: 0,
    // object or array
    data: undefined,
    // api message that could be used in SnackBar
    title: '',
    // using "undefined" as initial value could help us more easily
    // to check whether this api was success or not
    // ex: const data = (res.errors) ? res.errors : res.data
    errors: undefined,
    // additional information about this response
    meta: undefined,
  };
  const { meta, data } = params;
  const { status, url, method, params: queryParameters, body, headers } = meta;
  const { result, errorMessage, isSuccess } = data;

  finalRes.status = status;
  finalRes.meta = {
    method,
    url,
    params: queryParameters,
    body,
    headers,
  };
  if (!isSuccess) {
    finalRes.errors = has(errorMessage, 'message')
      ? [
          {
            type: 'fail',
            message: errorMessage.message,
            stack: errorMessage,
          },
        ]
      : [
          {
            type: 'fail',
            message: errorMessage.message,
          },
        ];
  } else {
    const warnReasons = [];
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
            warnReasons.push({
              message: `${pKey} has been changed to ${api.resConverter[resKey]}, please modify params!`,
            });
          }
        });
      });
      // checking the type of response data by api.response() definitions
      // we don't assign additional data of body for response here
      const data = {
        params: res,
        api: api.response(),
        body: {},
        definitions: api.definitions,
      };
      typeUtil(warnReasons, data);
    });
    finalRes.data = isArray(result) ? resList : resList[0];
    if (!isEmpty(warnReasons)) {
      warnReasons.forEach(warn => console.log(warn));
      finalRes.errors = warnReasons.map(warnReason => {
        return {
          type: 'warning',
          message: warnReason.message,
          stack: warnReason.stack,
        };
      });
    }
  }
  return finalRes;
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

const createAxios = () => {
  const instance = axios.create({
    validateStatus: status => status >= 200 && status < 300,
    // set an acceptable timeout (20 seconds) to avoid infinite request
    // this value is as same as `defaultCommandTimeout` of cypress.api.json
    timeout: 20000,
  });

  // get the query params object from url string
  const axiosParams = urlString => {
    const params = {};
    new URL(urlString).searchParams.forEach((value, key) => {
      params[key] = value;
    });
    return params;
  };

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
        meta: {
          status: response.status,
          url: response.config.url,
          method: response.config.method,
          params: axiosParams(response.request.responseURL),
          body: response.config.data,
          headers: response.headers,
        },
        data: {
          result: response.data,
          isSuccess: true,
        },
      };
    },
    error => {
      let errorRes = {};
      //initial the error response data
      errorRes.meta = {
        url: error.config.url,
        method: error.config.method,
        body: error.config.data,
      };
      // the request may not finished yet
      // we need to check if the request is defined to get the params
      if (errorRes.request) {
        errorRes.meta.params = axiosParams(errorRes.request.responseURL);
      }
      if (error.response) {
        const {
          status,
          headers,
          statusText,
          data: errorMessage,
        } = error.response;
        errorRes.meta.status = status;
        errorRes.meta.headers = headers;
        // the official format of error data from API should be
        // {apiUrl, code, message, stack}
        errorRes.data = {
          errorMessage: has(errorMessage, 'message')
            ? errorMessage
            : statusText,
          isSuccess: false,
        };
      } else if (error.request) {
        // had send a request but got no response
        // ex: server connection timeout
        errorRes.data = {
          errorMessage: error.message,
          isSuccess: false,
        };
      } else {
        // failed sending a request
        errorRes.data = {
          errorMessage: error.message,
          isSuccess: false,
        };
      }
      return errorRes;
    },
  );

  return instance;
};

export const axiosInstance = createAxios();

export const sleep = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};
