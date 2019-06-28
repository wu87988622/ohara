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
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';
import IconButton from '@material-ui/core/IconButton';
import toastr from 'toastr';
import { get } from 'lodash';
import { ConfirmModal } from 'components/common/Modal';

import * as topicApi from 'api/topicApi';
import * as MESSAGES from 'constants/messages';
import * as utils from '../WorkspacesDetailPageUtils';
import TopicNewModal from './TopicNewModal';
import { Main, NewButton, StyledTable, ActionIcon } from '../styles';
import { useSetState } from 'utils/hooks';

const Topics = props => {
  const { worker } = props;

  const headers = [
    'Topic name',
    'Partitions',
    'Replication factor',
    'Metrics (BytesInPerSec)',
    'Last modified',
    'Action',
  ];

  const [state, setState] = useSetState({
    isDeleting: false,
    isNewModalOpen: false,
    isDeleteModalOpen: false,
    topicToBeDeleted: '',
  });

  const [topics, setTopics, isLoading, fetchTopics] = utils.useFetchTopics(
    worker.brokerClusterName,
  );

  const handleDelete = async () => {
    setState({ isDeleting: true });
    const { topicToBeDeleted } = state;
    const res = await topicApi.deleteTopic(topicToBeDeleted);
    const isSuccess = get(res, 'data.isSuccess', false);
    setState({ isDeleting: false });

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

  const { isNewModalOpen, isDeleteModalOpen, isDeleting } = state;

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
        <StyledTable headers={headers} isLoading={isLoading}>
          {() => {
            return topics.map(topic => {
              const {
                name,
                numberOfPartitions,
                numberOfReplications,
                metrics,
                lastModified,
              } = topic;
              return (
                <TableRow key={name}>
                  <TableCell component="th" scope="row">
                    {name}
                  </TableCell>
                  <TableCell align="left">{numberOfPartitions}</TableCell>
                  <TableCell align="left">{numberOfReplications}</TableCell>
                  <TableCell align="left">
                    {utils.getMetrics(metrics)}
                  </TableCell>
                  <TableCell align="left">
                    {utils.getDateFromTimestamp(lastModified)}
                  </TableCell>
                  <TableCell align="right">
                    <IconButton
                      aria-label="Edit"
                      data-testid={topic.name}
                      onClick={() =>
                        setState({
                          isDeleteModalOpen: true,
                          topicToBeDeleted: name,
                        })
                      }
                    >
                      <ActionIcon className="fas fa-trash-alt" />
                    </IconButton>
                  </TableCell>
                </TableRow>
              );
            });
          }}
        </StyledTable>
      </Main>

      <TopicNewModal
        isActive={isNewModalOpen}
        onClose={() => {
          setState({ isNewModalOpen: false });
        }}
        onConfirm={fetchTopics}
        brokerClusterName={worker.brokerClusterName}
      />

      <ConfirmModal
        isActive={isDeleteModalOpen}
        title="Delete topic?"
        confirmBtnText="Yes, Delete this topic"
        cancelBtnText="No, Keep it"
        handleCancel={() => setState({ isDeleteModalOpen: false })}
        handleConfirm={handleDelete}
        isConfirmWorking={isDeleting}
        message="Are you sure you want to delete this topic? This action cannot be undone!"
        isDelete
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
