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
import Button from '@material-ui/core/Button';
import AppBar from '@material-ui/core/AppBar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import Toolbar from '@material-ui/core/Toolbar';
import KeyboardBackspaceIcon from '@material-ui/icons/KeyboardBackspace';

const StyledIconButton = styled(IconButton)`
  margin-right: ${props => props.theme.spacing(2)}px;
`;

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
    handleClose,
    hasSave = false,
    handleSave,
  } = props;
  return (
    <>
      <Dialog
        fullScreen
        open={open}
        onClose={handleClose}
        PaperProps={{
          style: {
            backgroundColor: '#f5f6fa',
          },
        }}
      >
        <AppBar>
          <Toolbar>
            <StyledIconButton
              edge="start"
              color="inherit"
              onClick={handleClose}
            >
              <KeyboardBackspaceIcon />
            </StyledIconButton>
            <StyledTypography variant="h6">{title}</StyledTypography>
            {hasSave && (
              <Button autoFocus color="inherit" onClick={handleSave}>
                save
              </Button>
            )}
          </Toolbar>
        </AppBar>
        <StyledDialogContent>{children}</StyledDialogContent>
      </Dialog>
    </>
  );
};

FullScreenDialog.propTypes = {
  children: PropTypes.any.isRequired,
  open: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
  hasSave: PropTypes.bool,
  handleSave: PropTypes.func,
};

export default FullScreenDialog;
