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
import styled from 'styled-components';
import Icon from '@material-ui/core/Icon';
import Snackbar from '@material-ui/core/Snackbar';
import IconButton from '@material-ui/core/IconButton';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import SnackbarContext from './SnackbarContext';

const SytledCloseIcon = styled(Icon)`
  font-size: 15px;
`;

const SnackbarProvider = props => {
  const [isOpen, setOpen] = useState();
  const [message, setMessage] = useState();
  const { autoClose = 5000, children } = props;

  const handleClose = (event, reason) => {
    if (reason === 'clickaway') {
      return;
    }

    setOpen(false);
  };

  const setSnackMessage = message => {
    setOpen(true);
    setMessage(message);
  };

  return (
    <SnackbarContext.Provider
      value={{
        isOpen,
        message,
        handleClose,
        autoClose,
        setSnackMessage,
      }}
    >
      <Snackbar
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        open={isOpen}
        autoHideDuration={autoClose}
        onClose={handleClose}
      >
        <SnackbarContent
          message={message}
          action={[
            <IconButton key="close" color="inherit" onClick={handleClose}>
              <SytledCloseIcon className="fas fa-times" />
            </IconButton>,
          ]}
        />
      </Snackbar>
      {children}
    </SnackbarContext.Provider>
  );
};

SnackbarProvider.propTypes = {
  children: PropTypes.object,
  message: PropTypes.string,
  autoClose: PropTypes.number,
};

export default SnackbarProvider;
