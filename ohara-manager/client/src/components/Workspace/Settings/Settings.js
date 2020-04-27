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
import { Dialog } from 'components/common/Dialog';

const Settings = () => {
  const { isOpen, close } = useEditWorkspaceDialog();
  const [isDeleteOpen, setIsDeleteOpen] = React.useState(false);
  const [selectedMenu, setSelectedMenu] = React.useState('');
  const [selectedComponent, setSelectedComponent] = React.useState(null);
  const scrollRef = React.useRef(null);

  const workspace = hooks.useWorkspace();

  const resetSelectedItem = () => {
    setSelectedComponent(null);
  };

  const { menu, sections } = useConfig({
    openDeleteProgressDialog: () => {
      setIsDeleteOpen(true);
      resetSelectedItem(null);
    },
    workspace,
  });

  const handleMenuClick = ({ text: selectedItem, ref }) => {
    scrollRef.current = ref.current;
    setSelectedMenu(selectedItem);
  };

  const handleComponentChange = newPage => {
    const { ref, heading: currentSection, type, name } = newPage;

    scrollRef.current = ref.current;
    setSelectedMenu(currentSection); // sync the menu selected state
    setSelectedComponent({ name, type });
  };

  // Use a different layout for rendering page component
  const isPageComponent =
    !!selectedComponent?.name &&
    selectedComponent?.type === SETTINGS_COMPONENT_TYPES.PAGE;

  React.useEffect(() => {
    if (!isPageComponent && scrollRef?.current) {
      scrollRef.current.scrollIntoView();
    }
  }, [isPageComponent, menu, scrollRef, sections, selectedComponent]);

  return (
    <StyledFullScreenDialog
      title="Settings"
      open={isOpen}
      onClose={close}
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

        <Dialog
          open={isDeleteOpen}
          title="abc"
          onClose={() => setIsDeleteOpen(false)}
          onConfirm={() => {}}
          children="Dumb dialog"
        />
      </Wrapper>
    </StyledFullScreenDialog>
  );
};

export default Settings;
