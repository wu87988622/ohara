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

import React, { useState, useRef } from 'react';
import PropTypes from 'prop-types';

import Menu from '@material-ui/core/Menu';
import MenuItem from '@material-ui/core/MenuItem';
import IconButton from '@material-ui/core/IconButton';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import MoreVertIcon from '@material-ui/icons/MoreVert';
import GetAppIcon from '@material-ui/icons/GetApp';
import DeleteForeverIcon from '@material-ui/icons/DeleteForever';

import * as hooks from 'hooks';
import { DeleteDialog } from 'components/common/Dialog';
import FileDownload from './FileDownload';

const FileActionsMenu = ({ file, deleteDisabled }) => {
  const [anchorEl, setAnchorEl] = useState(null);
  const downloadEl = useRef(null);

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
    setIsDeleteDialogOpen(false);
  };

  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const isWorking = hooks.useIsFileLoading();
  const deleteFile = hooks.useDeleteFileAction();

  const { name, url: fileUrl } = file;

  const handleDelete = async () => {
    if (name) {
      deleteFile(name);
      setAnchorEl(null);
      setIsDeleteDialogOpen(false);
    }
  };

  const handleDownload = () => {
    downloadEl.current.click();
    setTimeout(() => setAnchorEl(null), 500);
  };

  return (
    <>
      <IconButton onClick={handleClick}>
        <MoreVertIcon />
      </IconButton>
      <Menu
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={handleDownload}>
          <ListItemIcon>
            <GetAppIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText primary="Download" />
        </MenuItem>
        <MenuItem
          onClick={() => setIsDeleteDialogOpen(true)}
          disabled={deleteDisabled}
        >
          <ListItemIcon>
            <DeleteForeverIcon />
          </ListItemIcon>
          <ListItemText primary="Delete" />
        </MenuItem>
      </Menu>
      <FileDownload ref={downloadEl} url={fileUrl} name={name} />
      <DeleteDialog
        title="Delete file?"
        content={`Are you sure you want to delete the file: ${name} ? This action cannot be undone!`}
        open={isDeleteDialogOpen}
        handleClose={handleClose}
        handleConfirm={handleDelete}
        working={isWorking}
      />
    </>
  );
};

FileActionsMenu.propTypes = {
  file: PropTypes.PropTypes.shape({
    name: PropTypes.string.isRequired,
    group: PropTypes.string.isRequired,
    url: PropTypes.string.isRequired,
  }).isRequired,
  deleteDisabled: PropTypes.bool,
};
export default FileActionsMenu;
