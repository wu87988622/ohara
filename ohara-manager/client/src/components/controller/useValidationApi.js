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
import { useRef } from 'react';
import { get } from 'lodash';

import useSnackbar from 'components/context/Snackbar/useSnackbar';
import {
  handleError,
  axiosInstance,
  handleConnectorValidationError,
  handleNodeValidationError,
} from './apiUtils';
import * as URL from './url';

const useValidationApi = url => {
  const { showMessage } = useSnackbar();
  const resData = useRef();

  const request = async params => {
    try {
      const res = await axiosInstance.put(url, params);
      const isSuccess = get(res, 'data.isSuccess', false);

      switch (url) {
        case URL.VALIDATE_CONNECTOR_URL:
          const connectorMessage = handleConnectorValidationError(res);
          if (connectorMessage !== '') {
            showMessage(connectorMessage);
          }
          break;
        case URL.VALIDATE_NODE_URL:
          const nodeMessage = handleNodeValidationError(res);
          if (nodeMessage[0] !== '') {
            showMessage(nodeMessage);
          }
          break;
        default:
          return;
      }

      if (!isSuccess) {
        showMessage(handleError(res));
      }
      resData.current = res;
    } catch (err) {
      showMessage(handleError(err));
    }
  };

  const validationApi = async params => {
    await request(params);
  };

  const getData = () => resData.current;

  return { getData, validationApi };
};

export default useValidationApi;
