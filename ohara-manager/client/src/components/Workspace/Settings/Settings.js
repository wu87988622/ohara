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
import { SETTINGS_COMPONENT_TYPES, KIND } from 'const';
import { Wrapper, StyledFullScreenDialog } from './SettingsStyles';
import { useConfig } from './SettingsHooks';
import { DeleteWorkspace, RestartWorkspace } from './DangerZone';
import { Dialog } from 'components/common/Dialog';

const Settings = () => {
  const isSettingsOpen = hooks.useIsSettingsOpen();
  const pageName = hooks.usePageNameInSettings();
  const closeSettings = hooks.useCloseSettingsAction();
  const [selectedMenu, setSelectedMenu] = React.useState('');
  const [selectedComponent, setSelectedComponent] = React.useState(null);
  const [isConfirmDialogOpen, setIsConfirmDialogOpen] = React.useState(false);
  const scrollRef = React.useRef(null);

  const openRestartWorkspace = hooks.useOpenRestartWorkspaceDialogAction();
  const restartWorkspace = hooks.useRestartWorkspaceAction();
  const hasRunningServices = hooks.useHasRunningServices();
  const workspace = hooks.useWorkspace();
  const { shouldBeRestartWorkspace } = hooks.useShouldBeRestartWorkspace();

  const [isWorkspaceDeleting, setIsWorkspaceDeleting] = React.useState(false);

  const deleteWorkspace = () => setIsWorkspaceDeleting(true);

  const resetSelectedItem = () => {
    setSelectedComponent(null);
  };

  const { menu, sections } = useConfig({
    deleteWorkspace,
    openRestartWorkspaceProgressDialog: () => {
      resetSelectedItem();
      openRestartWorkspace();
      restartWorkspace(KIND.workspace);
    },
    openRestartBrokerProgressDialog: () => {
      resetSelectedItem();
      openRestartWorkspace({
        steps: ['Stop Worker', 'Stop Broker', 'Start Broker', 'Start Worker'],
      });
      restartWorkspace(KIND.broker);
    },
    openRestartWorkerProgressDialog: () => {
      resetSelectedItem();
      openRestartWorkspace({
        steps: ['Stop Worker', 'Start Worker'],
      });
      restartWorkspace(KIND.worker);
    },
    restartConfirmMessage: (kind) => hooks.useRestartConfirmMessage(kind),
    hasRunningServices,
    workspace,
  });

  const handleMenuClick = ({ text: selectedItem, ref }) => {
    scrollRef.current = ref.current;
    setSelectedMenu(selectedItem);
  };

  const handleComponentChange = (newPage) => {
    const { ref, heading: currentSection, type, name } = newPage;

    scrollRef.current = ref?.current;
    setSelectedMenu(currentSection); // sync the menu selected state
    setSelectedComponent({ name, type });
  };

  const handleClose = () => {
    if (shouldBeRestartWorkspace && selectedComponent?.name) {
      return setIsConfirmDialogOpen(true);
    }

    closeSettings();
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
      isPageComponent={isPageComponent}
      onClose={handleClose}
      open={isSettingsOpen}
      testId="workspace-settings-dialog"
      title="Settings"
    >
      <Wrapper>
        {
          // This feature is disabled, see #4659
          false && (
            <SettingsMenu
              closePageComponent={resetSelectedItem}
              handleClick={handleMenuClick}
              isPageComponent={isPageComponent}
              menu={menu}
              scrollRef={scrollRef}
              selected={selectedMenu}
            />
          )
        }

        <SettingsMain
          handleChange={handleComponentChange}
          handleClose={resetSelectedItem}
          sections={sections}
          selectedComponent={selectedComponent}
        />
        <RestartWorkspace />
        <DeleteWorkspace
          isOpen={isWorkspaceDeleting}
          onClose={() => {
            setIsWorkspaceDeleting(false);
            handleClose();
          }}
        />

        <Dialog
          confirmText="OK"
          maxWidth="xs"
          onClose={() => setIsConfirmDialogOpen(false)}
          onConfirm={() => {
            closeSettings();
            setIsConfirmDialogOpen(false);
          }}
          open={isConfirmDialogOpen}
          title="Leaving without restart"
        >
          The changes you made won't take effect unless you restart this
          workspace
        </Dialog>
      </Wrapper>
    </StyledFullScreenDialog>
  );
};

export default Settings;
