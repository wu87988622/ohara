import axios from 'axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const fetchHdfs = async () => {
  try {
    const res = await axios.get('/api/configuration/hdfs');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const validateHdfs = async ({ uri }) => {
  try {
    const res = await axios.put('/api/configuration/validate/hdfs', {
      uri,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const saveHdfs = async ({ name, uri }) => {
  try {
    const res = await axios.post('/api/configuration/hdfs/save', {
      name,
      uri,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};

export const deleteHdfs = async (uuid) => {
  try {
    const res = await axios.delete(`/api/configuration/hdfs/${uuid}`);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
