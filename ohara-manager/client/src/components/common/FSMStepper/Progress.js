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
import PropTypes from 'prop-types';
import { min, max } from 'lodash';
import Box from '@material-ui/core/Box';
import LinearProgress from '@material-ui/core/LinearProgress';
import Typography from '@material-ui/core/Typography';

// Valid value is 0 to 100
const getValidValue = (value) => {
  return min([max([0, value]), 100]);
};

const Progress = ({ state }) => {
  const { activeStep, forward = true, error, steps } = state.context;

  const isFinish = state.matches('finish');
  const isRevert = !forward;

  const color = !error ? 'primary' : 'secondary';
  const value = getValidValue(
    ((isRevert ? activeStep + 1 : activeStep) / steps.length) * 100,
  );
  const variant = !isFinish && isRevert ? 'query' : 'determinate';

  return (
    <Box alignItems="center" display="flex">
      <Box mr={1} width="100%">
        <LinearProgress color={color} value={value} variant={variant} />
      </Box>
      <Box minWidth={35}>
        <Typography color="textSecondary" variant="body2">{`${Math.round(
          value,
        )}%`}</Typography>
      </Box>
    </Box>
  );
};

Progress.propTypes = {
  state: PropTypes.shape({
    context: PropTypes.shape({
      activeStep: PropTypes.number.isRequired,
      error: PropTypes.object,
      forward: PropTypes.bool,
      steps: PropTypes.array.isRequired,
    }).isRequired,
    matches: PropTypes.func.isRequired,
  }).isRequired,
};

export default Progress;
