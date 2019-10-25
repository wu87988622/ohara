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

import React, { createContext, useContext, useReducer, useMemo } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import Snackbar from '@material-ui/core/Snackbar';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import Slide from '@material-ui/core/Slide';

const StyledCloseIcon = styled(Icon)`
  font-size: 15px;
`;

const SnackbarContext = createContext();

const SnackbarProvider = ({ children }) => {
  function snackbarReducer(state, action) {
    const { type, message } = action;
    switch (type) {
      case 'show':
        return { isOpen: true, message };
      case 'hide':
        return { isOpen: false, message: '' };
      default:
        throw new Error(`Unhandled action type: ${type}`);
    }
  }

  const [state, dispatch] = useReducer(snackbarReducer, {
    message: '',
    isOpen: false,
  });

  const handleClose = (event, reason) => {
    // Disable when a mouse click is on `body`, closing the
    // snackbar behavior
    if (reason === 'clickaway') return;

    dispatch({ type: 'hide' });
  };

  return (
    <SnackbarContext.Provider value={dispatch}>
      <Snackbar
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        open={state.isOpen}
        autoHideDuration={5000}
        onClose={handleClose}
        TransitionComponent={Slide}
      >
        <SnackbarContent
          message={state.message}
          action={[
            <IconButton key="close" color="inherit" onClick={handleClose}>
              <StyledCloseIcon className="fas fa-times" />
            </IconButton>,
          ]}
        />
      </Snackbar>
      {children}
    </SnackbarContext.Provider>
  );
};

const useSnackbar = () => {
  const dispatch = useContext(SnackbarContext);

  if (dispatch === undefined) {
    throw new Error('useSnackbar must be used within a SnackbarProvider');
  }

  // Since the consumers of this context will always want to
  // `show` the message, so we hide the implementaion details here,
  // also, wrapping the `dispatch` in a `useMemo` prevents it from
  // going into an infinite loop
  const showMessage = useMemo(
    () => message => dispatch({ type: 'show', message }),
    [dispatch],
  );

  return showMessage;
};

SnackbarProvider.propTypes = {
  children: PropTypes.any.isRequired,
};

export { SnackbarProvider, useSnackbar };
