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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { capitalize, toUpper } from 'lodash';
import moment from 'moment';
import ReactJson from 'react-json-view';

import Grid from '@material-ui/core/Grid';
import Paper from '@material-ui/core/Paper';
import Typography from '@material-ui/core/Typography';
import CircularProgress from '@material-ui/core/CircularProgress';

import { Dialog } from 'components/common/Dialog';
import { LOG_TYPES, STEP_STAGES, EVENTS } from './const';

const Divider = ({ text = '' }) => (
  <div className="divider">
    <span>{text}</span>
  </div>
);

Divider.propTypes = {
  text: PropTypes.string,
};

const LogViewer = ({ revertText, state }) => {
  const { activeStep, logs, steps } = state.context;
  const [currentLog, setCurrentLog] = useState(null);

  const isLoading = state.matches({ auto: 'loading' });
  const step = steps[activeStep];

  return (
    <Paper className="logViewer" variant="outlined">
      <Grid container>
        {logs.map((log) => {
          if (log.type === LOG_TYPES.EVENT) {
            return (
              <Grid item key={log.key} xs={12}>
                <Divider
                  text={log.title === EVENTS.REVERT ? revertText : log.title}
                />
              </Grid>
            );
          }
          return (
            <Grid container justify="space-between" key={log.key}>
              <Typography gutterBottom variant="body2">
                {moment(log.createdAt).format('YYYY/MM/DD hh:mm:ss')}{' '}
                {capitalize(log.title)}
              </Typography>

              {log.stepStage === STEP_STAGES.FAILURE ? (
                <Typography
                  className="log-step-stage"
                  color="secondary"
                  gutterBottom
                  onClick={() => setCurrentLog(log)}
                  variant="body2"
                >
                  ERROR
                </Typography>
              ) : (
                <Typography color="primary" gutterBottom variant="body2">
                  {log.isRevert ? toUpper(revertText) : '[OK]'}
                </Typography>
              )}
            </Grid>
          );
        })}

        {isLoading && (
          <Grid container justify="space-between">
            <Typography gutterBottom variant="body2">
              {moment(new Date()).format('YYYY/MM/DD hh:mm:ss')}{' '}
              {capitalize(step?.name)}
              ...
            </Typography>

            <CircularProgress size={12} />
          </Grid>
        )}
      </Grid>

      <Dialog
        maxWidth="md"
        onClose={() => setCurrentLog(null)}
        open={!!currentLog}
        showActions={false}
        title={`Failed to ${currentLog?.title}`}
      >
        <ReactJson
          displayDataTypes={false}
          displayObjectSize={false}
          enableClipboard={false}
          iconStyle="square"
          src={currentLog?.payload || {}}
        />
      </Dialog>
    </Paper>
  );
};

LogViewer.propTypes = {
  revertText: PropTypes.string,
  state: PropTypes.shape({
    context: PropTypes.shape({
      activeStep: PropTypes.number.isRequired,
      forward: PropTypes.bool,
      logs: PropTypes.array.isRequired,
      steps: PropTypes.array.isRequired,
    }).isRequired,
    matches: PropTypes.func.isRequired,
  }).isRequired,
};

LogViewer.defaultProps = {
  revertText: 'ROLLBACK',
};

export default LogViewer;
