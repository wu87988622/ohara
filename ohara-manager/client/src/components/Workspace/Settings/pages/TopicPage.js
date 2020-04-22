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
import { isEmpty } from 'lodash';

import Link from '@material-ui/core/Link';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import AddBox from '@material-ui/icons/AddBox';
import VisibilityIcon from '@material-ui/icons/Visibility';

import * as hooks from 'hooks';
import {
  useViewTopicDialog,
  useAddTopicDialog,
  useEditWorkspaceDialog,
} from 'context';
import Table from 'components/common/Table/MuiTable';
import ViewTopicDialog from 'components/Topic/ViewTopicDialog';
import AddTopicDialog from 'components/Topic/AddTopicDialog';
import { Wrapper } from './TopicPageStyles';

function TopicPage() {
  const broker = hooks.useBroker();
  const sharedTopics = hooks.useTopicsInWorkspace(true);
  const pipelineOnlyTopics = hooks.useTopicsInWorkspace(false);
  const switchPipeline = hooks.useSwitchPipelineAction();
  const { open: openAddTopicDialog } = useAddTopicDialog();
  const { open: openViewTopicDialog } = useViewTopicDialog();
  const { close: closeEditWorkspaceDialog } = useEditWorkspaceDialog();

  return (
    <Wrapper>
      <div className="shared-topics" data-testid="shared-topics">
        <Table
          title="Shared topics"
          actions={[
            {
              icon: () => <AddBox />,
              tooltip: 'Add Topic',
              isFreeAction: true,
              onClick: openAddTopicDialog,
              disabled: broker?.state !== 'RUNNING',
            },
          ]}
          columns={[
            { title: 'Name', field: 'name' },
            {
              title: 'Partitions',
              field: 'numberOfPartitions',
            },
            {
              title: 'Replications',
              field: 'numberOfReplications',
            },
            // Completed in the next version, so hide it first
            // 'Used by pipelines',
            {
              title: 'State',
              field: 'state',
            },
            {
              title: 'Actions',
              cellStyle: { textAlign: 'right' },
              headerStyle: { textAlign: 'right' },
              sorting: false,
              render: topic => (
                <Tooltip title="View Topic">
                  <IconButton
                    data-testid={`view-topic-${topic.name}`}
                    onClick={() => openViewTopicDialog(topic)}
                  >
                    <VisibilityIcon />
                  </IconButton>
                </Tooltip>
              ),
            },
          ]}
          data={sharedTopics}
          options={{
            paging: false,
            search: true,
          }}
        />
      </div>

      {!isEmpty(pipelineOnlyTopics) && (
        <div className="pipeline-only-topics">
          <Table
            title="Pipeline only topics"
            columns={[
              { title: 'Name', field: 'tags.displayName' },
              {
                title: 'Partitions',
                field: 'numberOfPartitions',
              },
              {
                title: 'Replications',
                field: 'numberOfReplications',
              },
              {
                title: 'State',
                field: 'state',
              },
              {
                title: 'Used',
                field: 'tags.pipelineName',
                render: topic => {
                  return (
                    <Link
                      component="button"
                      variant="h6"
                      onClick={() => {
                        if (topic?.tags?.pipelineName) {
                          closeEditWorkspaceDialog();
                          switchPipeline(topic?.tags?.pipelineName);
                        }
                      }}
                    >
                      {topic?.tags?.pipelineName}
                    </Link>
                  );
                },
              },
              {
                title: 'Actions',
                cellStyle: { textAlign: 'right' },
                headerStyle: { textAlign: 'right' },
                sorting: false,
                render: topic => (
                  <Tooltip title="View Topic">
                    <IconButton
                      data-testid={`view-topic-${topic.name}`}
                      onClick={() => openViewTopicDialog(topic)}
                    >
                      <VisibilityIcon />
                    </IconButton>
                  </Tooltip>
                ),
              },
            ]}
            data={pipelineOnlyTopics}
            options={{
              paging: false,
              search: true,
            }}
          />
        </div>
      )}

      <AddTopicDialog uniqueId="settings" />
      <ViewTopicDialog />
    </Wrapper>
  );
}

export default TopicPage;
