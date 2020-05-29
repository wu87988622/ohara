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

import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';

import AddIcon from '@material-ui/icons/Add';
import ClearIcon from '@material-ui/icons/Clear';
import CloudUploadIcon from '@material-ui/icons/CloudUpload';
import CloudDownloadIcon from '@material-ui/icons/CloudDownload';
import CreateIcon from '@material-ui/icons/Create';
import DeleteIcon from '@material-ui/icons/Delete';
import EditIcon from '@material-ui/icons/Edit';
import SettingsBackupRestoreIcon from '@material-ui/icons/SettingsBackupRestore';
import VisibilityIcon from '@material-ui/icons/Visibility';

const defaultIcons = {
  add: AddIcon,
  create: <CreateIcon />,
  delete: <DeleteIcon />,
  download: <CloudDownloadIcon />,
  edit: <EditIcon />,
  upload: <CloudUploadIcon />,
  undo: <SettingsBackupRestoreIcon />,
  remove: <ClearIcon />,
  view: <VisibilityIcon />,
};

const defaultNames = Object.keys(defaultIcons);

function Action(props) {
  const { action } = props;

  if (!action) {
    return null;
  }

  if (action.hidden) {
    return null;
  }

  const disabled = action.disabled || props.disabled;

  const handleClick = event => {
    if (action.onClick) {
      action.onClick(props.data);
      event.stopPropagation();
    }
  };

  const icon = defaultIcons[action.name];

  const button = (
    <IconButton
      component="div"
      data-testid={action.testid}
      disabled={disabled}
      onClick={handleClick}
    >
      {icon}
    </IconButton>
  );

  if (action.tooltip) {
    return disabled ? (
      <Tooltip title={action.tooltip}>
        <span>{button}</span>
      </Tooltip>
    ) : (
      <Tooltip title={action.tooltip}>{button}</Tooltip>
    );
  } else {
    return button;
  }
}

Action.propTypes = {
  action: PropTypes.shape({
    disabled: PropTypes.bool,
    hidden: PropTypes.bool,
    name: PropTypes.oneOf(defaultNames).isRequired,
    onClick: PropTypes.func,
    tooltip: PropTypes.string,
    testid: PropTypes.string,
  }),
  data: PropTypes.object,
  disabled: PropTypes.bool,
};

Action.defaultProps = {
  action: {
    disabled: false,
    hidden: false,
  },
  data: {},
  disabled: false,
};

export default Action;
