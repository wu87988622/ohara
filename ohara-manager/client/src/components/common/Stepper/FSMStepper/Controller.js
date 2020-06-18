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

import Grid from '@material-ui/core/Grid';
import Button from '@material-ui/core/Button';

import ArrowBackIcon from '@material-ui/icons/ArrowBack';
import StopIcon from '@material-ui/icons/Stop';
import ReplayIcon from '@material-ui/icons/Replay';
import PlayArrowIcon from '@material-ui/icons/PlayArrow';

import ConfirmDialog from 'components/common/Dialog/DeleteDialog';

const Controller = (props) => {
  const { rollback: allowRollback, state, send } = props;
  const isForwardProcessing = !!state?.context?.forward;

  const showResumeButton = allowRollback && state.matches('idle');
  const showRetryButton = state.matches('auto.failure');
  const showRollbackButton =
    allowRollback &&
    isForwardProcessing &&
    (state.matches('idle') || state.matches('finish'));
  const showSuspendButton = allowRollback && state.matches('auto');

  return (
    <>
      <Grid
        alignItems="center"
        container
        direction="row"
        justify="center"
        spacing={2}
      >
        {showSuspendButton && (
          <Grid item>
            <Button
              color="primary"
              onClick={() => send('SUSPEND')}
              startIcon={<StopIcon />}
            >
              SUSPEND
            </Button>
          </Grid>
        )}

        {showResumeButton && (
          <Grid item>
            <Button
              color="primary"
              onClick={() => send('RESUME')}
              startIcon={<PlayArrowIcon />}
            >
              RESUME
            </Button>
          </Grid>
        )}

        {showRetryButton && (
          <Grid item>
            <Button
              color="primary"
              onClick={() => send('RETRY')}
              startIcon={<ReplayIcon />}
            >
              RETRY
            </Button>
          </Grid>
        )}

        {showRollbackButton && (
          <Grid item>
            <Button
              color="primary"
              onClick={() => send('REVERT')}
              startIcon={<ArrowBackIcon />}
            >
              REVERT
            </Button>
          </Grid>
        )}
      </Grid>
      <ConfirmDialog
        cancelText="Disagree"
        confirmText="Agree"
        content="Do you want to suspend?"
        isWorking={state.matches('auto.cancelling')}
        onClose={() => send('DISAGREE')}
        onConfirm={() => send('AGREE')}
        open={
          state.matches('auto.cancelConfirming') ||
          state.matches('auto.cancelling')
        }
        title="Do you want to suspend?"
      />
    </>
  );
};

Controller.propTypes = {
  rollback: PropTypes.bool,
  state: PropTypes.shape({
    context: PropTypes.shape({
      activeIndex: PropTypes.number,
      activities: PropTypes.array,
      error: PropTypes.object,
      forward: PropTypes.bool,
    }),
    matches: PropTypes.func,
  }).isRequired,
  send: PropTypes.func.isRequired,
};

Controller.defaultProps = {
  rollback: false,
};

export default Controller;
