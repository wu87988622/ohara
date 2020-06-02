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
import { noop } from 'lodash';

import MuiPopover from '@material-ui/core/Popover';
import { Tooltip } from 'components/common/Tooltip';

const Popover = React.forwardRef((props, ref) => {
  const [anchorEl, setAnchorEl] = React.useState(null);
  const open = Boolean(anchorEl);
  const id = open ? props.id : undefined;

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
    props.onOpen();
  };

  const handleClose = () => {
    setAnchorEl(null);
    props.onClose();
  };

  // Apis
  const apis = { close: handleClose };

  React.useImperativeHandle(ref, () => apis);

  const Trigger = React.cloneElement(props.trigger, {
    'aria-describedby': id,
    onClick: handleClick,
  });

  return (
    <>
      {(props.showTooltip && (
        <Tooltip title={props.tooltipTitle}>{Trigger}</Tooltip>
      )) ||
        Trigger}
      <MuiPopover
        anchorEl={anchorEl}
        anchorOrigin={props.anchorOrigin}
        id={id}
        onClose={handleClose}
        open={open}
        transformOrigin={props.transformOrigin}
      >
        {props.children}
      </MuiPopover>
    </>
  );
});

Popover.propTypes = {
  trigger: PropTypes.element.isRequired,
  children: PropTypes.element.isRequired,
  id: PropTypes.string,
  anchorOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
  transformOrigin: PropTypes.shape({
    vertical: PropTypes.string,
    horizontal: PropTypes.string,
  }),
  showTooltip: PropTypes.bool,
  tooltipTitle: PropTypes.string,
  onOpen: PropTypes.func,
  onClose: PropTypes.func,
};

Popover.defaultProps = {
  id: 'popover',
  anchorOrigin: {
    vertical: 'bottom',
    horizontal: 'center',
  },
  transformOrigin: {
    vertical: 'top',
    horizontal: 'center',
  },
  showTooltip: false,
  tooltipTitle: 'This is a tooltip',
  onOpen: noop,
  onClose: noop,
};

export default Popover;
