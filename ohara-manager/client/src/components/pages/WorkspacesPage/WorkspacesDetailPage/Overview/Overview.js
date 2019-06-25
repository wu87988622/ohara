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
import Paper from '@material-ui/core/Paper';
import Grid from '@material-ui/core/Grid';
import { makeStyles } from '@material-ui/core/styles';

import OverviewTopics from './OverviewTopics';
import OverviewConnectors from './OverviewConnectors';
import OverviewStreamApps from './OverviewStreamApps';
import OverviewNodes from './OverviewNodes';
import { TabHeading, List, StyledIcon } from './styles';

const useStyles = makeStyles(theme => ({
  root: {
    flexGrow: 1,
    margin: theme.spacing(2),
  },
  paper: {
    color: theme.palette.text.secondary,
  },
}));

const Overview = props => {
  const { worker } = props;
  const { imageName, brokerClusterName, name: workerName, connectors } = worker;

  const classes = useStyles();

  const handleRedirect = service => {
    props.history.push(service);
  };

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item sm={6}>
          <Paper className={classes.paper}>
            <TabHeading>
              <StyledIcon className="fas fa-info-circle" />
              <span className="title">Basic info</span>
            </TabHeading>
            <List>
              <li>Image: {imageName}</li>
            </List>
          </Paper>
        </Grid>

        <Grid item sm={6}>
          <Paper className={classes.paper}>
            <OverviewConnectors connectors={connectors} />
          </Paper>
        </Grid>

        <Grid item sm={6}>
          <Paper className={classes.paper}>
            <OverviewNodes worker={worker} handleRedirect={handleRedirect} />
          </Paper>
        </Grid>

        <Grid item sm={6}>
          <Paper className={classes.paper}>
            <OverviewStreamApps
              workerName={workerName}
              handleRedirect={handleRedirect}
            />
          </Paper>
        </Grid>

        <Grid item sm={6}>
          <Paper className={classes.paper}>
            <OverviewTopics
              handleRedirect={handleRedirect}
              brokerClusterName={brokerClusterName}
            />
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
};

Overview.propTypes = {
  history: PropTypes.shape({
    push: PropTypes.func.isRequired,
  }).isRequired,
  worker: PropTypes.object.isRequired,
};

export default Overview;
