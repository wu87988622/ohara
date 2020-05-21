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
import { join, map, includes, isEmpty, isFunction, toUpper } from 'lodash';

import Link from '@material-ui/core/Link';
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
  const {
    broker,
    topics: data,
    onCreate,
    onDelete,
    onLinkClick,
    title,
  } = props;

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
                disabled={!isEmpty(topic.pipelines)}
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
          {
            title: 'Name',
            customFilterAndSearch: (filterValue, topic) => {
              return includes(toUpper(topic.displayName), toUpper(filterValue));
            },
            render: topic => {
              return topic.displayName;
            },
          },
          {
            title: 'Partitions',
            field: 'numberOfPartitions',
            type: 'numeric',
          },
          {
            title: 'Replications',
            field: 'numberOfReplications',
            type: 'numeric',
          },
          {
            title: 'Pipelines',
            customFilterAndSearch: (filterValue, topic) => {
              const value = join(
                map(topic?.pipelines, pipeline => pipeline.name),
              );
              return includes(toUpper(value), toUpper(filterValue));
            },
            render: topic => {
              return (
                <>
                  {map(topic?.pipelines, pipeline => (
                    <div key={pipeline.name}>
                      <Tooltip title="Click the link to switch to that pipeline">
                        <Link
                          component="button"
                          variant="body2"
                          onClick={() => onLinkClick(pipeline)}
                        >
                          {pipeline.name}
                        </Link>
                      </Tooltip>
                    </div>
                  ))}
                </>
              );
            },
          },
          {
            title: 'State',
            field: 'state',
          },
          renderActionColumn(),
        ]}
        data={data}
        options={{
          prompt: options?.prompt,
          rowStyle: null,
          showTitle: options?.showTitle,
        }}
      />
      <TopicCreateDialog
        topics={data}
        broker={broker}
        isOpen={isCreateDialogOpen}
        onClose={() => setIsCreateDialogOpen(false)}
        onConfirm={onCreate}
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
      pipelines: PropTypes.arrayOf(
        PropTypes.shape({
          name: PropTypes.string,
        }),
      ),
      state: PropTypes.string,
      tags: PropTypes.shape({
        displayName: PropTypes.string,
        isShared: PropTypes.bool,
      }),
    }),
  ),
  onCreate: PropTypes.func,
  onDelete: PropTypes.func,
  onLinkClick: PropTypes.func,
  options: PropTypes.shape({
    onCreateIconClick: PropTypes.func,
    onDeleteIconClick: PropTypes.func,
    onDetailIconClick: PropTypes.func,
    prompt: PropTypes.string,
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
  onLinkClick: () => {},
  options: defaultOptions,
  title: 'Topics',
};

export default TopicTable;
