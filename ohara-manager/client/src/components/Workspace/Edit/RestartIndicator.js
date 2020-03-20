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

import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import Collapse from '@material-ui/core/Collapse';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import Typography from '@material-ui/core/Typography';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import WarningIcon from '@material-ui/icons/Warning';
import * as hooks from 'hooks';
import { DeleteDialog } from 'components/common/Dialog';
import { Wrapper } from './RestartIndicatorStyles';

RestartIndicator.propTypes = {
  invisible: PropTypes.bool,
};

RestartIndicator.defaultProps = {
  invisible: true,
};

function RestartIndicator({ invisible }) {
  const currWk = hooks.useWorker();
  const currBk = hooks.useBroker();
  const currZk = hooks.useZookeeper();
  const isWkFetching = false;
  const isBkFetching = false;
  const isZkFetching = false;
  const updateWkStagingSettings = () => {};
  const updateBkStagingSettings = () => {};
  const updateZkStagingSettings = () => {};

  const [isDiscardConfirmOpen, setIsDiscardConfirmOpen] = useState(false);
  const [isRestartConfirmOpen, setIsRestartConfirmOpen] = useState(false);

  const handleDiscard = async () => {
    await Promise.all([
      updateWkStagingSettings(currWk.settings),
      updateBkStagingSettings(currBk.settings),
      updateZkStagingSettings(currZk.settings),
    ]);
    setIsDiscardConfirmOpen(false);
  };

  const handleRestart = async () => {
    // TODO: The logic to restart the workspace is more complicated, move to the next version
    setIsRestartConfirmOpen(false);
  };

  return (
    <Collapse in={!invisible}>
      <Wrapper>
        <SnackbarContent
          message={
            <Grid container alignItems="center">
              <WarningIcon fontSize="small" />
              <Typography className="message">
                You’ve made some changes to the workspace. Please restart for
                these settings to take effect!!
              </Typography>
            </Grid>
          }
          action={
            <>
              <Button
                size="small"
                onClick={() => setIsDiscardConfirmOpen(true)}
              >
                discard
              </Button>
              <Button
                size="small"
                onClick={() => setIsRestartConfirmOpen(true)}
              >
                restart
              </Button>
            </>
          }
        />
        <DeleteDialog
          title="Discard Workspace?"
          content={`Are you sure you want to discard the workspace, This action cannot be undone!`}
          open={isDiscardConfirmOpen}
          handleClose={() => setIsDiscardConfirmOpen(false)}
          handleConfirm={handleDiscard}
          confirmText="DISCARD"
          isWorking={isWkFetching || isBkFetching || isZkFetching}
        />
        <Dialog
          open={isRestartConfirmOpen}
          onClose={() => setIsRestartConfirmOpen(false)}
        >
          <DialogTitle>Not available !!</DialogTitle>
          <DialogContent>
            <DialogContentText>
              The function of restarting the workspace will be completed in the
              next version.
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleRestart} color="primary" autoFocus>
              CLOSE
            </Button>
          </DialogActions>
        </Dialog>
      </Wrapper>
    </Collapse>
  );
}

export default RestartIndicator;
