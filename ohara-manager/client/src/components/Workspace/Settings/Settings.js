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
import { Dialog } from 'components/common/Dialog';

const Settings = () => {
  const {
    isOpen: isEditWorkspaceDialogOpen,
    close: closeEditWorkspaceDialog,
    data: pageName,
  } = useEditWorkspaceDialog();
  const [selectedMenu, setSelectedMenu] = React.useState('');
  const [selectedComponent, setSelectedComponent] = React.useState(null);
  const [isConfirmDialogOpen, setIsConfirmDialogOpen] = React.useState(false);
  const scrollRef = React.useRef(null);

  const openDeleteWorkspace = hooks.useOpenDeleteWorkspaceDialogAction();
  const deleteWorkspace = hooks.useDeleteWorkspaceAction();
  const openRestartWorkspace = hooks.useOpenRestartWorkspaceDialogAction();
  const restartWorkspace = hooks.useRestartWorkspaceAction();
  const restartConfirmMessage = hooks.useRestartConfirmMessage();
  const hasRunningServices = hooks.useHasRunningServices();
  const workspace = hooks.useWorkspace();
  const { shouldBeRestartWorkspace } = hooks.useShouldBeRestartWorkspace();

  const resetSelectedItem = () => {
    setSelectedComponent(null);
  };

  const { menu, sections } = useConfig({
    openDeleteProgressDialog: () => {
      resetSelectedItem();
      openDeleteWorkspace();
      deleteWorkspace({
        // after delete workspace
        // this Settings Dialog should be closed
        onSuccess: () => closeEditWorkspaceDialog(),
      });
    },
    openRestartProgressDialog: () => {
      resetSelectedItem();
      openRestartWorkspace();
      restartWorkspace();
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
    if (shouldBeRestartWorkspace && selectedComponent?.name) {
      return setIsConfirmDialogOpen(true);
    }

    closeEditWorkspaceDialog();
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
      open={isEditWorkspaceDialogOpen}
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

        <Dialog
          open={isConfirmDialogOpen}
          onClose={() => setIsConfirmDialogOpen(false)}
          onConfirm={() => {
            closeEditWorkspaceDialog();
            setIsConfirmDialogOpen(false);
          }}
          confirmText="OK"
          title="Leaving without restart"
          maxWidth="xs"
        >
          The changes you made won't take effect unless you restart this
          workspace
        </Dialog>
      </Wrapper>
    </StyledFullScreenDialog>
  );
};

export default Settings;
