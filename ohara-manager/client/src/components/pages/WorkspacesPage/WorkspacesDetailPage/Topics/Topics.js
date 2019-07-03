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
import toastr from 'toastr';
import { get } from 'lodash';

import * as topicApi from 'api/topicApi';
import * as MESSAGES from 'constants/messages';
import * as utils from '../WorkspacesDetailPageUtils';
import TopicNewModal from './TopicNewModal';
import { AlertDialog } from 'components/common/Mui/Dialog';
import { Main, NewButton, ActionIcon } from '../styles';
import { SortTable } from 'components/common/Mui/Table';
import { useSetState } from 'utils/hooks';

const Topics = props => {
  const { worker } = props;

  const {
    topics,
    setTopics,
    loading: fetchingTopics,
    fetchTopics,
  } = utils.useFetchTopics(worker.brokerClusterName);

  const headRows = [
    { id: 'name', label: 'Topic name' },
    { id: 'partitions', label: 'Partitions' },
    { id: 'replication', label: 'Replication factor' },
    { id: 'metrics', label: 'Metrics (BytesInPerSec)' },
    { id: 'lastModified', label: 'Last modified' },
    { id: 'action', label: 'Action' },
  ];

  const actionButton = name => {
    return (
      <IconButton
        aria-label="Edit"
        data-testid={name}
        onClick={() =>
          setState({
            isDeleteModalOpen: true,
            topicToBeDeleted: name,
          })
        }
      >
        <ActionIcon className="fas fa-trash-alt" />
      </IconButton>
    );
  };

  const rows = topics.map(topic => {
    const {
      name,
      numberOfPartitions,
      numberOfReplications,
      metrics,
      lastModified,
    } = topic;
    return {
      name: name,
      partitions: numberOfPartitions,
      replication: numberOfReplications,
      metrics: utils.getMetrics(metrics),
      lastModified: utils.getDateFromTimestamp(lastModified),
      action: actionButton(name),
    };
  });

  const [state, setState] = useSetState({
    deleting: false,
    isNewModalOpen: false,
    isDeleteModalOpen: false,
    topicToBeDeleted: '',
  });

  const handleDelete = async () => {
    setState({ deleting: true });
    const { topicToBeDeleted } = state;
    const res = await topicApi.deleteTopic(topicToBeDeleted);
    const isSuccess = get(res, 'data.isSuccess', false);
    setState({ deleting: false });

    if (isSuccess) {
      toastr.success(`${MESSAGES.TOPIC_DELETION_SUCCESS} ${topicToBeDeleted}`);
      const updatedTopics = topics.filter(
        topic => topic.name !== topicToBeDeleted,
      );

      setState({
        isDeleteModalOpen: false,
        topicToBeDeleted: '',
      });

      setTopics(updatedTopics);
    }
  };

  const { isNewModalOpen, isDeleteModalOpen, deleting } = state;

  return (
    <>
      <NewButton
        text="New topic"
        data-testid="new-topic"
        onClick={() => {
          setState({ isNewModalOpen: true });
        }}
      />

      <Main>
        <SortTable
          isLoading={fetchingTopics}
          headRows={headRows}
          rows={rows}
          tableName="topic"
        />
      </Main>

      <TopicNewModal
        isActive={isNewModalOpen}
        onClose={() => {
          setState({ isNewModalOpen: false });
        }}
        onConfirm={fetchTopics}
        brokerClusterName={worker.brokerClusterName}
      />

      <AlertDialog
        title="Delete topic?"
        content="Are you sure you want to delete this topic? This action cannot be undone!"
        open={isDeleteModalOpen}
        handleClose={() => setState({ isDeleteModalOpen: false })}
        handleConfirm={handleDelete}
        working={deleting}
        testId="delete-topic-dialog"
      />
    </>
  );
};

Topics.propTypes = {
  match: PropTypes.shape({
    url: PropTypes.string.isRequired,
  }).isRequired,
  worker: PropTypes.shape({
    brokerClusterName: PropTypes.string.isRequired,
  }).isRequired,
};

export default Topics;
