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
import styled from 'styled-components';
import DialogContent from '@material-ui/core/DialogContent';
import Dialog from '@material-ui/core/Dialog';
import AppBar from '@material-ui/core/AppBar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import Toolbar from '@material-ui/core/Toolbar';
import CloseIcon from '@material-ui/icons/Close';

const StyledDialogContent = styled(DialogContent)`
  max-width: 1400px;
  width: 1024px;
  margin: ${props => props.theme.spacing(12)}px auto;
  padding: 0 ${props => props.theme.spacing(3)}px;

  /* We want to use browser's scrollbar instead of DialogContent's */
  overflow-y: visible;
`;

const StyledTypography = styled(Typography)`
  flex: 1;
`;

const FullScreenDialog = props => {
  const {
    title,
    children,
    open,
    onClose,
    testId = 'fullscreen-dialog',
    ...other
  } = props;
  return (
    <Dialog
      fullScreen
      open={open}
      onClose={onClose}
      PaperProps={{
        style: {
          backgroundColor: '#f5f6fa',
        },
      }}
      data-testid={testId}
      {...other}
    >
      <AppBar>
        <Toolbar>
          <StyledTypography variant="h4">{title}</StyledTypography>
          <IconButton
            color="inherit"
            data-testid={`${testId}-close-button`}
            onClick={onClose}
          >
            <CloseIcon />
          </IconButton>
        </Toolbar>
      </AppBar>
      <StyledDialogContent>
        <div className="dialog-inner">{children}</div>
      </StyledDialogContent>
    </Dialog>
  );
};

FullScreenDialog.propTypes = {
  children: PropTypes.node.isRequired,
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  testId: PropTypes.string,
};

export default FullScreenDialog;
