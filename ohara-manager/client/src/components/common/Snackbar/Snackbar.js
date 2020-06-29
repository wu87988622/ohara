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

import { noop } from 'lodash';
import React from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Snackbar from '@material-ui/core/Snackbar';
import IconButton from '@material-ui/core/IconButton';
import SnackbarContent from '@material-ui/core/SnackbarContent';
import Fade from '@material-ui/core/Fade';
import CloseIcon from '@material-ui/icons/Close';
import { mapStateToProps } from './SnackbarContainer';
import * as actions from 'store/actions';

const SnackBar = (props) => {
  const handleClose = (event, reason) => {
    // Disable when a mouse click is on `body`, closing the
    // snackbar behavior
    if (reason === 'clickaway') return;

    props.dispatch(actions.hideMessage.trigger());
  };

  return (
    <Snackbar
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
      autoHideDuration={5000}
      data-testid="snackbar"
      onClose={handleClose}
      open={props.isOpen}
      TransitionComponent={Fade}
    >
      <SnackbarContent
        action={[
          <IconButton
            color="inherit"
            key="close"
            onClick={handleClose}
            size="small"
          >
            <CloseIcon fontSize="small" />
          </IconButton>,
        ]}
        message={props.message}
      />
    </Snackbar>
  );
};

SnackBar.propTypes = {
  message: PropTypes.string,
  isOpen: PropTypes.bool,
  dispatch: PropTypes.func,
};

SnackBar.defaultProps = {
  message: '',
  isOpen: false,
  dispatch: noop,
};

const StyledSnackbar = connect(mapStateToProps)(SnackBar);
export default StyledSnackbar;
