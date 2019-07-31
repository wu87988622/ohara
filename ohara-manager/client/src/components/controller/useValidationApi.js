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
