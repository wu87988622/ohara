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
import Tooltip from '@material-ui/core/Tooltip';

import OverviewTable from './OverviewTable';
import { useFetchTopics } from '../WorkspacesDetailPageUtils';
import {
  TabHeading,
  StyledTableCell,
  StyledIcon,
  StyledIconLink,
} from './styles';

const OverviewTopics = props => {
  const { handleRedirect, brokerClusterName } = props;
  const { topics, loading: fetchingTopics } = useFetchTopics(brokerClusterName);
  return (
    <>
      <TabHeading>
        <StyledIcon className="fas fa-list-ul" />
        <span className="title">Topics</span>
        <StyledIconLink onClick={() => handleRedirect('topics')}>
          <Tooltip title={`Link to Topics page`} enterDelay={1000}>
            <StyledIcon
              className="fas fa-external-link-square-alt"
              data-testid="overview-topics-link"
            />
          </Tooltip>
        </StyledIconLink>
      </TabHeading>
      <OverviewTable
        headers={['Name', 'Partitions', 'Replication factor']}
        isLoading={fetchingTopics}
      >
        {topics.map(topic => {
          return (
            <TableRow key={topic.name}>
              <StyledTableCell component="th" scope="row">
                {topic.name}
              </StyledTableCell>
              <StyledTableCell
                align="left"
                data-testid={`${topic.name}-nop-${topic.numberOfPartitions}`}
              >
                {topic.numberOfPartitions}
              </StyledTableCell>
              <StyledTableCell
                align="right"
                data-testid={`${topic.name}-nor-${topic.numberOfReplications}`}
              >
                {topic.numberOfReplications}
              </StyledTableCell>
            </TableRow>
          );
        })}
      </OverviewTable>
    </>
  );
};

OverviewTopics.propTypes = {
  handleRedirect: PropTypes.func.isRequired,
  brokerClusterName: PropTypes.string.isRequired,
};

export default OverviewTopics;
