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
import moment from 'moment';
import toastr from 'toastr';
import { get } from 'lodash';
import { ConfirmModal } from 'components/common/Modal';

import * as topicApi from 'api/topicApi';
import * as MESSAGES from 'constants/messages';
import TopicNewModal from '../../TopicNewModal';
import { StyledTable, StyledIcon, StyledButton } from './styles';
import { useSetState } from 'utils/hooks';
import { useFetchTopics } from '../WorkspacesDetailPageUtils';

const Topics = props => {
  const { worker } = props;

  const headers = [
    'Topic name',
    'Partitions',
    'Replication factor',
    'Metrics',
    'Last modified',
    'Action',
  ];

  const [state, setState] = useSetState({
    isDeleting: false,
    isNewModalOpen: false,
    isDeleteModalOpen: false,
    topicToBeDeleted: '',
  });

  const [topics, setTopics, isLoading, fetchTopics] = useFetchTopics(
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

  const getDateFromTimestamp = timestamp => {
    return moment.unix(timestamp / 1000).format('YYYY-MM-DD HH:mm:ss');
  };

  const { isNewModalOpen, isDeleteModalOpen, isDeleting } = state;

  return (
    <>
      <StyledButton
        text="New topic"
        data-testid="new-topic"
        onClick={() => {
          setState({ isNewModalOpen: true });
        }}
      />

      <StyledTable headers={headers} isLoading={isLoading}>
        {() => {
          return topics.map(topic => {
            return (
              <TableRow key={topic.name}>
                <TableCell component="th" scope="row">
                  {topic.name}
                </TableCell>
                <TableCell align="left">{topic.numberOfPartitions}</TableCell>
                <TableCell align="left">{topic.numberOfReplications}</TableCell>
                <TableCell align="left">451 bytes / secondes</TableCell>
                <TableCell align="left">
                  {getDateFromTimestamp(topic.lastModified)}
                </TableCell>
                <TableCell align="right">
                  <IconButton
                    aria-label="Edit"
                    data-testid={topic.name}
                    onClick={() =>
                      setState({
                        isDeleteModalOpen: true,
                        topicToBeDeleted: topic.name,
                      })
                    }
                  >
                    <StyledIcon className="fas fa-trash-alt" />
                  </IconButton>
                </TableCell>
              </TableRow>
            );
          });
        }}
      </StyledTable>

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
