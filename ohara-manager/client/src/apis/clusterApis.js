import axios from 'axios';

import { handleError } from 'utils/apiUtils';
import * as _ from 'utils/commonUtils';

export const fetchCluster = async () => {
  try {
    const res = await axios.get('/api/cluster');
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) {
      handleError(res);
    }

    return res;
  } catch (err) {
    handleError(err);
  }
};
