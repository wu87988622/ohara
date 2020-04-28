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
import styled, { css } from 'styled-components';

import Button from '@material-ui/core/Button';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import Typography from '@material-ui/core/Typography';
import WarningIcon from '@material-ui/icons/Warning';

import { DeleteDialog } from 'components/common/Dialog';

export const StyledSnackbarContent = styled(SnackbarContent)(
  ({ theme }) => css`
    background-color: ${theme.palette.warning.main};

    .alert {
      display: flex;
      align-items: center;
      letter-spacing: 0.1px;
      font-weight: 500;

      .alert-icon {
        opacity: 0.9;
        margin-right: ${theme.spacing(1)}px;
      }
    }

    .MuiButton-root {
      color: ${theme.palette.common.white};
    }
  `,
);

function RestartIndicator({ isOpen, onDiscard, onRestart }) {
  const [isDiscardConfirmDialogOpen, setIsDiscardConfirmDialogOpen] = useState(
    false,
  );
  const [isRestartConfirmDialogOpen, setIsRestartConfirmDialogOpen] = useState(
    false,
  );

  return (
    <div>
      {isOpen && (
        <StyledSnackbarContent
          message={
            <Typography className="alert" component="span">
              <WarningIcon className="alert-icon" fontSize="small" />
              You’ve made some changes to the workspace. Please restart for
              these settings to take effect!!
            </Typography>
          }
          action={
            <>
              <Button
                onClick={() => setIsDiscardConfirmDialogOpen(true)}
                size="small"
              >
                discard
              </Button>
              <Button
                onClick={() => setIsRestartConfirmDialogOpen(true)}
                size="small"
              >
                restart
              </Button>
            </>
          }
        />
      )}
      <DeleteDialog
        content="Are you sure you want to discard the workspace settings?"
        confirmText="DISCARD"
        onConfirm={() => {
          onDiscard();
          setIsDiscardConfirmDialogOpen(false);
        }}
        onClose={() => setIsDiscardConfirmDialogOpen(false)}
        open={isDiscardConfirmDialogOpen}
        title="Are you absolutely sure?"
      />
      <DeleteDialog
        content="This action cannot be undone. This will permanently restart the workspace.name zookeeper, broker, and worker."
        confirmText="RESTART"
        onConfirm={() => {
          onRestart();
          setIsRestartConfirmDialogOpen(false);
        }}
        onClose={() => setIsRestartConfirmDialogOpen(false)}
        open={isRestartConfirmDialogOpen}
        title="Are you absolutely sure?"
      />
    </div>
  );
}

RestartIndicator.propTypes = {
  isOpen: PropTypes.bool,
  onDiscard: PropTypes.func,
  onRestart: PropTypes.func,
};

RestartIndicator.defaultProps = {
  isOpen: false,
  onDiscard: () => {},
  onRestart: () => {},
};

export default RestartIndicator;
