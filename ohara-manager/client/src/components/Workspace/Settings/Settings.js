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
import { DeleteWorkspace } from './DangerZone';
import { getKeyWithId } from 'utils/object';

const Settings = () => {
  const { isOpen, close, data: pageName } = useEditWorkspaceDialog();
  const [selectedMenu, setSelectedMenu] = React.useState('');
  const [selectedComponent, setSelectedComponent] = React.useState(null);
  const scrollRef = React.useRef(null);

  const openDeleteWorkspace = hooks.useOpenDeleteWorkspaceDialogAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const workspaceId = hooks.useWorkspaceId();
  const zookeeperId = hooks.useZookeeperId();
  const brokerId = hooks.useBrokerId();
  const workerId = hooks.useWorkerId();
  const workspace = hooks.useWorkspace();

  const resetSelectedItem = () => {
    setSelectedComponent(null);
  };

  const { menu, sections } = useConfig({
    openDeleteProgressDialog: () => {
      resetSelectedItem(null);
      openDeleteWorkspace();
      deleteWorkspace({
        workspace: getKeyWithId(workspaceId),
        zookeeper: getKeyWithId(zookeeperId),
        broker: getKeyWithId(brokerId),
        worker: getKeyWithId(workerId),
      });
    },
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
        <DeleteWorkspace />
      </Wrapper>
    </StyledFullScreenDialog>
  );
};

export default Settings;
