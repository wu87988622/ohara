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
import { round } from 'lodash';

import Grid from '@material-ui/core/Grid';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';
import LinearProgress from '@material-ui/core/LinearProgress';
import NumberFormat from 'react-number-format';

const ResourceItem = ({ name, value, unit, used }) => {
  const roundValue = round(value);
  const roundUsed = round(used * 100, 1);
  return (
    <>
      <Grid container justify="space-between" alignItems="center">
        <Grid item xs={6}>
          <Typography>{name}</Typography>
        </Grid>
        <Grid item xs={6}>
          <Tooltip
            title={`${roundUsed}% used`}
            enterDelay={500}
            placement="right"
          >
            <Typography align="right" component="div">
              <NumberFormat
                value={roundValue}
                displayType="text"
                suffix={` ${unit}`}
                thousandSeparator
              />

              <LinearProgress
                variant="determinate"
                color={roundUsed < 80 ? 'primary' : 'secondary'}
                value={roundUsed}
              />
            </Typography>
          </Tooltip>
        </Grid>
      </Grid>
    </>
  );
};

ResourceItem.propTypes = {
  name: PropTypes.string.isRequired,
  value: PropTypes.number.isRequired,
  unit: PropTypes.string.isRequired,
  used: PropTypes.number.isRequired,
};

export default ResourceItem;
