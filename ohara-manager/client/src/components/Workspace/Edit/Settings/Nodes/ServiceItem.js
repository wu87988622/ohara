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

import Chip from '@material-ui/core/Chip';
import Grid from '@material-ui/core/Grid';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';

const ServiceItem = ({ name: serviceName, clusterKey, self = false }) => {
  const { name, group } = clusterKey;
  return (
    <>
      <Grid container justify="space-between" alignItems="center">
        <Grid item xs={6}>
          <Chip
            color={self ? 'primary' : 'default'}
            variant="outlined"
            size="small"
            label={serviceName}
          />
        </Grid>
        <Grid item xs={6}>
          <Typography align="right" component="div">
            <Tooltip
              title={`${group} / ${name}`}
              enterDelay={500}
              placement="right"
            >
              <span> {name}</span>
            </Tooltip>
          </Typography>
        </Grid>
      </Grid>
    </>
  );
};

ServiceItem.propTypes = {
  name: PropTypes.string.isRequired,
  clusterKey: PropTypes.shape({
    group: PropTypes.string.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
  self: PropTypes.bool,
};

export default ServiceItem;
