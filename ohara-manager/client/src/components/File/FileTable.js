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
import { find, isFunction, reject, some, sortBy, unionBy } from 'lodash';
import moment from 'moment';
import NumberFormat from 'react-number-format';

import Checkbox from '@material-ui/core/Checkbox';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';

import AddIcon from '@material-ui/icons/Add';
import CheckIcon from '@material-ui/icons/Check';
import ClearIcon from '@material-ui/icons/Clear';
import CloudUploadIcon from '@material-ui/icons/CloudUpload';
import CloudDownloadIcon from '@material-ui/icons/CloudDownload';
import DeleteIcon from '@material-ui/icons/Delete';
import SettingsBackupRestoreIcon from '@material-ui/icons/SettingsBackupRestore';

import Table from 'components/common/Table/MuiTable';
import FileDeleteDialog from './FileDeleteDialog';
import FileRemoveDialog from './FileRemoveDialog';
import FileDownload from './FileDownload';
import FileUpload from './FileUpload';

const defaultOptions = {
  comparison: false,
  comparedFiles: [],
  onAddIconClick: null,
  onDeleteIconClick: null,
  onDownloadIconClick: null,
  onUndoIconClick: null,
  onUploadIconClick: null,
  onRemoveIconClick: null,
  selection: false,
  selectedFiles: [],
  showAddIcon: false,
  showDeleteIcon: true,
  showDownloadIcon: true,
  showUndoIcon: false,
  showUploadIcon: true,
  showRemoveIcon: false,
  showTitle: true,
  showUsedColumn: false,
};

function FileTable(props) {
  const {
    files,
    onDelete,
    onUpload,
    onRemove,
    onSelectionChange,
    title,
  } = props;
  const options = { ...defaultOptions, ...props.options };

  const fileUploadRef = useRef(null);
  const fileDownloadRef = useRef(null);

  const [activeFile, setActiveFile] = useState();
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [isRemoveDialogOpen, setIsRemoveDialogOpen] = useState(false);
  const [selected, setSelected] = useState(options?.selectedFiles || []);

  const data = options?.comparison
    ? sortBy(unionBy(options?.comparedFiles, files, 'name'), ['name'])
    : files;

  const willBeRemoved = file => !find(files, f => f.name === file.name);

  const willBeAdded = file =>
    !find(options?.comparedFiles, f => f.name === file.name);

  const handleAddIconClick = () => {
    if (isFunction(options?.onAddIconClick)) {
      options.onAddIconClick();
    }
  };

  const handleDeleteIconClick = file => {
    if (isFunction(options?.onDeleteIconClick)) {
      options.onDeleteIconClick(file);
    } else {
      setIsDeleteDialogOpen(true);
      setActiveFile(file);
    }
  };

  const handleDownloadIconClick = file => {
    if (isFunction(options?.onDownloadIconClick)) {
      options.onDownloadIconClick(file);
    } else {
      setActiveFile(file);
      setTimeout(() => fileDownloadRef.current.click(), 500);
    }
  };

  const handleUndoIconClick = file => {
    if (isFunction(options?.onUndoIconClick)) {
      options.onUndoIconClick(file);
    }
  };

  const handleRemoveIconClick = file => {
    if (isFunction(options?.onRemoveIconClick)) {
      options.onRemoveIconClick(file);
    } else {
      setIsRemoveDialogOpen(true);
      setActiveFile(file);
    }
  };

  const handleUploadIconClick = () => {
    if (isFunction(options?.onUploadIconClick)) {
      options.onUploadIconClick();
    } else {
      fileUploadRef.current.click();
    }
  };

  const handleDeleteDialogConfirm = fileToDelete => {
    onDelete(fileToDelete);
    setIsDeleteDialogOpen(false);
  };

  const handleRemoveDialogConfirm = fileToRemove => {
    onRemove(fileToRemove);
    setIsRemoveDialogOpen(false);
  };

  const handleRowSelected = (event, dataClicked) => {
    if (dataClicked) {
      const newSelected = some(selected, f => f.name === dataClicked.name)
        ? reject(selected, f => f.name === dataClicked.name)
        : [...selected, dataClicked];

      setSelected(newSelected);
      onSelectionChange(newSelected, dataClicked);
    }
    event.stopPropagation();
  };

  const renderSelectionColumn = () => {
    const style = { paddingLeft: '0px', paddingRight: '0px', width: '42px' };
    return {
      cellStyle: style,
      headerStyle: style,
      hidden: !options?.selection,
      render: file => (
        <Checkbox
          checked={some(selected, f => f.name === file.name)}
          color="primary"
          onChange={event => handleRowSelected(event, file)}
        />
      ),
    };
  };

  const renderRowActions = () => {
    const isShow =
      options?.showDeleteIcon ||
      options?.showDownloadIcon ||
      options?.showRemoveIcon;

    const render = file => {
      const getUndoTooltipTitle = file => {
        if (willBeAdded(file)) {
          return 'Undo add file';
        } else if (willBeRemoved(file)) {
          return 'Undo remove file';
        }
        return 'Undo';
      };

      const showUndoIcon = file =>
        (options?.comparison && willBeAdded(file)) || willBeRemoved(file);

      const showRemoveIcon = file =>
        options?.showRemoveIcon && !showUndoIcon(file);

      return (
        <>
          {options?.showDownloadIcon && (
            <Tooltip title="Download file">
              <IconButton
                onClick={() => {
                  handleDownloadIconClick(file);
                }}
              >
                <CloudDownloadIcon />
              </IconButton>
            </Tooltip>
          )}
          {options?.showDeleteIcon && (
            <Tooltip title="Delete file">
              <IconButton
                onClick={() => {
                  handleDeleteIconClick(file);
                }}
              >
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          )}
          {showRemoveIcon(file) && (
            <Tooltip title="Remove file">
              <IconButton
                onClick={() => {
                  handleRemoveIconClick(file);
                }}
              >
                <ClearIcon />
              </IconButton>
            </Tooltip>
          )}
          {showUndoIcon(file) && (
            <Tooltip title={getUndoTooltipTitle(file)}>
              <IconButton
                onClick={() => {
                  handleUndoIconClick(file);
                }}
              >
                <SettingsBackupRestoreIcon />
              </IconButton>
            </Tooltip>
          )}
        </>
      );
    };

    return {
      title: 'Actions',
      cellStyle: { textAlign: 'right' },
      headerStyle: { textAlign: 'right' },
      hidden: !isShow,
      render,
      sorting: false,
    };
  };

  const getRowStyle = file => {
    if (options?.comparison && willBeRemoved(file)) {
      return {
        backgroundColor: 'rgba(255, 117, 159, 0.1)',
      };
    } else if (options?.comparison && willBeAdded(file)) {
      return {
        backgroundColor: 'rgba(114, 204, 255, 0.1)',
      };
    }
    return null;
  };

  return (
    <>
      <Table
        actions={[
          {
            icon: () => <AddIcon />,
            tooltip: 'Add File',
            hidden: !options?.showAddIcon,
            isFreeAction: true,
            onClick: handleAddIconClick,
          },
          {
            icon: () => <CloudUploadIcon />,
            tooltip: 'Upload File',
            hidden: !options?.showUploadIcon,
            isFreeAction: true,
            onClick: handleUploadIconClick,
          },
        ]}
        columns={[
          renderSelectionColumn(),
          { title: 'Name', field: 'name' },
          {
            title: 'Used',
            render: file => {
              if (file.isUsed) return <CheckIcon className="checkIcon" />;
            },
            hidden: !options?.showUsedColumn,
          },
          {
            title: 'File size(KB)',
            type: 'numeric',
            field: 'size',
            render: file => (
              <NumberFormat
                value={file.size}
                displayType="text"
                thousandSeparator
              />
            ),
          },
          {
            title: 'Last modified',
            type: 'date',
            field: 'lastModified',
            render: file => {
              return (
                <Tooltip
                  title={moment(file?.lastModified).format(
                    'YYYY/MM/DD HH:mm:ss',
                  )}
                >
                  <Typography>
                    {moment(file?.lastModified).fromNow()}
                  </Typography>
                </Tooltip>
              );
            },
          },
          renderRowActions(),
        ]}
        data={data}
        options={{
          paging: false,
          search: true,
          showTitle: options?.showTitle,
          rowStyle: file => getRowStyle(file),
        }}
        title={title}
      />
      <FileDeleteDialog
        isOpen={isDeleteDialogOpen}
        file={activeFile}
        onClose={() => setIsDeleteDialogOpen(false)}
        onConfirm={handleDeleteDialogConfirm}
      />
      <FileRemoveDialog
        isOpen={isRemoveDialogOpen}
        file={activeFile}
        onClose={() => setIsRemoveDialogOpen(false)}
        onConfirm={handleRemoveDialogConfirm}
      />
      <FileUpload ref={fileUploadRef} onUpload={onUpload} />
      <FileDownload file={activeFile} ref={fileDownloadRef} />
    </>
  );
}

FileTable.propTypes = {
  files: PropTypes.arrayOf(
    PropTypes.shape({
      isUsed: PropTypes.bool,
      lastModified: PropTypes.number,
      name: PropTypes.string,
      size: PropTypes.number,
    }),
  ),
  onDelete: PropTypes.func,
  onRemove: PropTypes.func,
  onSelectionChange: PropTypes.func,
  onUpload: PropTypes.func,
  options: PropTypes.shape({
    comparison: PropTypes.bool,
    comparedFiles: PropTypes.array,
    mode: PropTypes.string,
    onAddIconClick: PropTypes.func,
    onDeleteIconClick: PropTypes.func,
    onDownloadIconClick: PropTypes.func,
    onUndoIconClick: PropTypes.func,
    onUploadIconClick: PropTypes.func,
    onRemoveIconClick: PropTypes.func,
    selection: PropTypes.bool,
    selectedFiles: PropTypes.array,
    showAddIcon: PropTypes.bool,
    showDeleteIcon: PropTypes.bool,
    showDownloadIcon: PropTypes.bool,
    showUndoIcon: PropTypes.bool,
    showUploadIcon: PropTypes.bool,
    showRemoveIcon: PropTypes.bool,
    showTitle: PropTypes.bool,
    showUsedColumn: PropTypes.bool,
  }),
  title: PropTypes.string,
};

FileTable.defaultProps = {
  files: [],
  onDelete: () => {},
  onRemove: () => {},
  onSelectionChange: () => {},
  onUpload: () => {},
  options: defaultOptions,
  title: 'Files',
};

export default FileTable;
