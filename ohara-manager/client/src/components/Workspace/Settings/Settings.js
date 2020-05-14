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

import * as hooks from 'hooks';
import SettingsMain from './SettingsMain';
import SettingsMenu from './SettingsMenu';
import { SETTINGS_COMPONENT_TYPES } from 'const';
import { useEditWorkspaceDialog } from 'context';
import { Wrapper, StyledFullScreenDialog } from './SettingsStyles';
import { useConfig } from './SettingsHooks';
import { DeleteWorkspace, RestartWorkspace } from './DangerZone';

import { convertIdToKey } from 'utils/object';
import { CLUSTER_STATE } from 'const';

const Settings = () => {
  const { isOpen, close, data: pageName } = useEditWorkspaceDialog();
  const [selectedMenu, setSelectedMenu] = React.useState('');
  const [selectedComponent, setSelectedComponent] = React.useState(null);
  const scrollRef = React.useRef(null);

  const openDeleteWorkspace = hooks.useOpenDeleteWorkspaceDialogAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const openRestartWorkspace = hooks.useOpenRestartWorkspaceDialogAction();
  const restartWorkspace = hooks.useRestartWorkspaceAction();
  const workspace = hooks.useWorkspace();
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const tmpWorker = hooks.useWorker();
  const tmpBroker = hooks.useBroker();
  const tmpZookeeper = hooks.useZookeeper();
  const connectors = hooks.useConnectors();
  const topics = hooks.useTopicsInWorkspace();
  const streams = hooks.useStreams();
  const shabondis = hooks.useShabondis();
  const files = hooks.useFiles();

  const hasRunningConnectors = connectors.find(
    connector => connector.state === CLUSTER_STATE.RUNNING,
  );
  const hasRunningStreams = streams.find(
    stream => stream.state === CLUSTER_STATE.RUNNING,
  );
  const hasRunningShabondis = shabondis.find(
    shabondi => shabondi.state === CLUSTER_STATE.RUNNING,
  );
  let hasRunningServices = false;
  let restartConfirmMessage =
    'This will restart the zookeeper, broker and worker.';

  if (hasRunningConnectors || hasRunningStreams || hasRunningShabondis) {
    restartConfirmMessage = `Oops, there are still some running services in ${workspace.name}. You should stop them first and then you will be able to restart this workspace.`;
    hasRunningServices = true;
  }
  const resetSelectedItem = () => {
    setSelectedComponent(null);
  };

  const { menu, sections } = useConfig({
    openDeleteProgressDialog: () => {
      resetSelectedItem(null);
      openDeleteWorkspace();
      deleteWorkspace({
        workspace: convertIdToKey(workspaceId),
        zookeeper: convertIdToKey(zookeeperId),
        broker: convertIdToKey(brokerId),
        worker: convertIdToKey(workerId),
        files,
      });
    },
    openRestartProgressDialog: () => {
      resetSelectedItem(null);
      openRestartWorkspace();
      restartWorkspace({
        workspace: convertIdToKey(workspaceId),
        zookeeper: convertIdToKey(zookeeperId),
        broker: convertIdToKey(brokerId),
        worker: convertIdToKey(workerId),
        workerSettings: workspace.worker,
        brokerSettings: workspace.broker,
        zookeeperSettings: workspace.zookeeper,
        tmpWorker,
        tmpBroker,
        tmpZookeeper,
        topics,
      });
    },
    restartConfirmMessage,
    hasRunningServices,
    workspace,
  });

  const handleMenuClick = ({ text: selectedItem, ref }) => {
    scrollRef.current = ref.current;
    setSelectedMenu(selectedItem);
  };

  const handleComponentChange = newPage => {
    const { ref, heading: currentSection, type, name } = newPage;

    scrollRef.current = ref?.current;
    setSelectedMenu(currentSection); // sync the menu selected state
    setSelectedComponent({ name, type });
  };

  const handleClose = () => {
    close();
    resetSelectedItem();
  };

  // Use a different layout for rendering page component
  const isPageComponent =
    !!selectedComponent?.name &&
    selectedComponent?.type === SETTINGS_COMPONENT_TYPES.PAGE;

  React.useEffect(() => {
    if (!isPageComponent && scrollRef?.current) {
      scrollRef.current.scrollIntoView();
    }
  }, [isPageComponent]);

  React.useEffect(() => {
    if (pageName && pageName !== 'settings') {
      handleComponentChange({
        name: pageName,
        type: SETTINGS_COMPONENT_TYPES.PAGE,
      });
    }
  }, [pageName]);

  return (
    <StyledFullScreenDialog
      title="Settings"
      open={isOpen}
      onClose={handleClose}
      testId="edit-workspace-dialog"
      isPageComponent={isPageComponent}
    >
      <Wrapper>
        {// This feature is disabled, see #4659
        false && (
          <SettingsMenu
            menu={menu}
            selected={selectedMenu}
            handleClick={handleMenuClick}
            closePageComponent={resetSelectedItem}
            isPageComponent={isPageComponent}
            scrollRef={scrollRef}
          />
        )}

        <SettingsMain
          sections={sections}
          handleChange={handleComponentChange}
          handleClose={resetSelectedItem}
          selectedComponent={selectedComponent}
        />
        <RestartWorkspace />
        <DeleteWorkspace />
      </Wrapper>
    </StyledFullScreenDialog>
  );
};

export default Settings;
