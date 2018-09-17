import axios from 'axios';

import { handleError } from 'utils/apiHelpers';
import { get } from 'utils/helpers';

export const fetchCluster = async () => {
  try {
    const res = await axios.get('/api/cluster');
    const isSuccess = get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
