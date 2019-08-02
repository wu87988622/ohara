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
import { get } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as utils from '../WorkspacesDetailPageUtils';
import TopicNewModal from './TopicNewModal';
import { DeleteDialog } from 'components/common/Mui/Dialog';
import { Main, NewButton, ActionIcon } from './styles';
import { SortTable } from 'components/common/Mui/Table';
import { useSetState } from 'utils/hooks';
import * as useApi from 'components/controller';
import * as URL from 'components/controller/url';
import useSnackbar from 'components/context/Snackbar/useSnackbar';

const Topics = props => {
  const { worker } = props;

  const { showMessage } = useSnackbar();
  const {
    data: topics,
    isLoading: fetchingTopics,
    refetch,
  } = useApi.useFetchApi(URL.TOPIC_URL);
  const {
    getData: deleteTopicRes,
    deleteApi: deleteTopic,
  } = useApi.useDeleteApi(URL.TOPIC_URL);
  const topicsUnderBrokerCluster = get(topics, 'data.result', []).filter(
    topic => topic.brokerClusterName === worker.brokerClusterName,
  );
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
      <Tooltip title={`Delete ${name}`} enterDelay={1000}>
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
      </Tooltip>
    );
  };

  const rows = topicsUnderBrokerCluster.map(topic => {
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
    await deleteTopic(topicToBeDeleted);
    const isSuccess = get(deleteTopicRes(), 'data.isSuccess', false);
    setState({ deleting: false });

    if (isSuccess) {
      showMessage(`${MESSAGES.TOPIC_DELETION_SUCCESS} ${topicToBeDeleted}`);

      setState({
        isDeleteModalOpen: false,
        topicToBeDeleted: '',
      });
      refetch();
    }
  };

  const {
    isNewModalOpen,
    isDeleteModalOpen,
    deleting,
    topicToBeDeleted,
  } = state;

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
        onConfirm={() => {
          refetch();
        }}
        brokerClusterName={worker.brokerClusterName}
      />

      <DeleteDialog
        title="Delete topic?"
        content={`Are you sure you want to delete the topic: ${topicToBeDeleted} ? This action cannot be undone!`}
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
