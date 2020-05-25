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

import { TopicTable } from 'components/Topic';
import * as context from 'context';
import * as hooks from 'hooks';

function SharedTopicTable() {
  const broker = hooks.useBroker();
  const topics = hooks.useTopicsInWorkspace(true);
  const createAndStartTopic = hooks.useCreateAndStartTopicAction();
  const stopAndDeleteTopic = hooks.useStopAndDeleteTopicAction();
  const switchPipeline = hooks.useSwitchPipelineAction();
  const { close: closeSettingsDialog } = context.useEditWorkspaceDialog();

  const handleCreate = topicToCreate => {
    return createAndStartTopic({
      ...topicToCreate,
      displayName: topicToCreate.name,
      isShared: true,
      numberOfPartitions: Number(topicToCreate?.numberOfPartitions),
      numberOfReplications: Number(topicToCreate?.numberOfReplications),
    });
  };

  const handleDelete = topicToDelete => {
    return stopAndDeleteTopic(topicToDelete);
  };

  const handleLinkClick = pipelineClicked => {
    if (pipelineClicked?.name) {
      closeSettingsDialog();
      switchPipeline(pipelineClicked.name);
    }
  };

  return (
    <>
      <TopicTable
        broker={broker}
        topics={topics}
        onCreate={handleCreate}
        onDelete={handleDelete}
        onLinkClick={handleLinkClick}
        title="Shared topics"
      />
    </>
  );
}

export default SharedTopicTable;
