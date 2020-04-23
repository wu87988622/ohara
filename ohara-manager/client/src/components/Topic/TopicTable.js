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
import { isFunction } from 'lodash';

import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';

import CreateIcon from '@material-ui/icons/Create';
import DeleteIcon from '@material-ui/icons/Delete';

import VisibilityIcon from '@material-ui/icons/Visibility';

import Table from 'components/common/Table/MuiTable';
import TopicCreateDialog from './TopicCreateDialog';
import TopicDeleteDialog from './TopicDeleteDialog';
import TopicDetailDialog from './TopicDetailDialog';

const defaultOptions = {
  onCreateIconClick: null,
  onDeleteIconClick: null,
  onDetailIconClick: null,
  showCreateIcon: true,
  showDeleteIcon: true,
  showDetailIcon: true,
  showTitle: true,
};

function TopicTable(props) {
  const { broker, topics: data, onCreate, onDelete, title } = props;

  const options = { ...defaultOptions, ...props?.options };

  const [activeTopic, setActiveTopic] = useState();
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false);
  const [isDetailDialogOpen, setIsDetailDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);

  const handleCreateIconClick = () => {
    if (isFunction(options?.onCreateIconClick)) {
      options.onCreateIconClick();
    } else {
      setIsCreateDialogOpen(true);
    }
  };

  const handleDeleteIconClick = topic => {
    if (isFunction(options?.onDeleteIconClick)) {
      options.onDeleteIconClick(topic);
    } else {
      setIsDeleteDialogOpen(true);
      setActiveTopic(topic);
    }
  };

  const handleDetailIconClick = topic => {
    if (isFunction(options?.onDetailIconClick)) {
      options.onDetailIconClick(topic);
    } else {
      setIsDetailDialogOpen(true);
      setActiveTopic(topic);
    }
  };

  const handleCreateDialogConfirm = topicToCreate => {
    onCreate(topicToCreate);
    setIsCreateDialogOpen(false);
  };

  const handleDeleteDialogConfirm = topicToDelete => {
    onDelete(topicToDelete);
    setIsDeleteDialogOpen(false);
  };

  const renderActionColumn = () => {
    const isShow = options?.showDeleteIcon || options?.showDetailIcon;

    const render = topic => {
      return (
        <>
          {options?.showDetailIcon && (
            <Tooltip title="View topic">
              <IconButton
                onClick={() => {
                  handleDetailIconClick(topic);
                }}
              >
                <VisibilityIcon />
              </IconButton>
            </Tooltip>
          )}
          {options?.showDeleteIcon && (
            <Tooltip title="Delete topic">
              <IconButton
                onClick={() => {
                  handleDeleteIconClick(topic);
                }}
              >
                <DeleteIcon />
              </IconButton>
            </Tooltip>
          )}
        </>
      );
    };

    return {
      cellStyle: { textAlign: 'right' },
      headerStyle: { textAlign: 'right' },
      hidden: !isShow,
      render,
      sorting: false,
      title: 'Actions',
    };
  };

  return (
    <>
      <Table
        title={title}
        actions={[
          {
            disabled: broker?.state !== 'RUNNING',
            hidden: !options?.showCreateIcon,
            icon: () => <CreateIcon />,
            isFreeAction: true,
            onClick: handleCreateIconClick,
            tooltip: 'Create Topic',
          },
        ]}
        columns={[
          { field: 'name', title: 'Name' },
          {
            field: 'numberOfPartitions',
            title: 'Partitions',
            type: 'numeric',
          },
          {
            field: 'numberOfReplications',
            title: 'Replications',
            type: 'numeric',
          },
          // Completed in the next version, so hide it first
          // 'Used by pipelines',
          {
            field: 'state',
            title: 'State',
          },
          renderActionColumn(),
        ]}
        data={data}
        options={{
          paging: false,
          search: true,
          showTitle: options?.showTitle,
        }}
      />
      <TopicCreateDialog
        broker={broker}
        isOpen={isCreateDialogOpen}
        onClose={() => setIsCreateDialogOpen(false)}
        onConfirm={handleCreateDialogConfirm}
      />
      <TopicDeleteDialog
        isOpen={isDeleteDialogOpen}
        topic={activeTopic}
        onClose={() => setIsDeleteDialogOpen(false)}
        onConfirm={handleDeleteDialogConfirm}
      />
      <TopicDetailDialog
        isOpen={isDetailDialogOpen}
        topic={activeTopic}
        onClose={() => setIsDetailDialogOpen(false)}
      />
    </>
  );
}

TopicTable.propTypes = {
  broker: PropTypes.shape({
    state: PropTypes.string,
  }),
  topics: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string,
      numberOfPartitions: PropTypes.number,
      numberOfReplications: PropTypes.number,
      state: PropTypes.string,
    }),
  ),
  onCreate: PropTypes.func,
  onDelete: PropTypes.func,
  options: PropTypes.shape({
    onCreateIconClick: PropTypes.func,
    onDeleteIconClick: PropTypes.func,
    onDetailIconClick: PropTypes.func,
    showCreateIcon: PropTypes.bool,
    showDeleteIcon: PropTypes.bool,
    showDetailIcon: PropTypes.bool,
    showTitle: PropTypes.bool,
  }),
  title: PropTypes.string,
};

TopicTable.defaultProps = {
  broker: null,
  topics: [],
  onCreate: () => {},
  onDelete: () => {},
  options: defaultOptions,
  title: 'Topics',
};

export default TopicTable;
