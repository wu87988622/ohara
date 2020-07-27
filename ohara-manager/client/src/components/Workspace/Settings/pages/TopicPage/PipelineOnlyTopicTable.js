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
import * as hooks from 'hooks';

function PipelineOnlyTopicTable() {
  const broker = hooks.useBroker();
  const topics = hooks.useTopicsInWorkspace(false);
  const switchPipeline = hooks.useSwitchPipelineAction();
  const settingsDialog = hooks.useWorkspaceSettingsDialog();

  const handleLinkClick = (pipelineClicked) => {
    if (pipelineClicked?.name) {
      settingsDialog.close();
      switchPipeline(pipelineClicked.name);
    }
  };

  return (
    <>
      <TopicTable
        broker={broker}
        onLinkClick={handleLinkClick}
        options={{
          showCreateIcon: false,
          showDeleteIcon: false,
        }}
        title="Pipeline only topics"
        topics={topics}
      />
    </>
  );
}

export default PipelineOnlyTopicTable;
