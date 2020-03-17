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

import React from 'react';
import { isString } from 'lodash';
import LinearProgress from '@material-ui/core/LinearProgress';
import Typography from '@material-ui/core/Typography';

import { Tooltip } from 'components/common/Tooltip';

const Percentage = (values, key) => {
  if (!isString(values)) {
    return values;
  }
  if (values.indexOf(key) < 0) {
    if (values.indexOf('null') > -1) {
      return 'None';
    }
    return values;
  }
  const splitValues = values.split(key);
  const value = splitValues[0];
  const used = Number(splitValues[1]);
  const color = used > 80 ? 'secondary' : 'primary';
  return (
    <>
      <Typography variant="subtitle2">{value}</Typography>
      <Tooltip title={`${used} %`}>
        <LinearProgress value={used} variant="determinate" color={color} />
      </Tooltip>
    </>
  );
};

export default Percentage;
