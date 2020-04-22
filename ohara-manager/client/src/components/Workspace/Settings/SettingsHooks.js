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
import AssignmentIcon from '@material-ui/icons/Assignment';
import VpnKeyIcon from '@material-ui/icons/VpnKey';
import LibraryBooksIcon from '@material-ui/icons/LibraryBooks';
import SettingsInputHdmiIcon from '@material-ui/icons/SettingsInputHdmi';
import WavesIcon from '@material-ui/icons/Waves';
import WarningIcon from '@material-ui/icons/Warning';
import StorageIcon from '@material-ui/icons/Storage';
import SubjectIcon from '@material-ui/icons/Subject';
import FolderOpenIcon from '@material-ui/icons/FolderOpen';
import TuneIcon from '@material-ui/icons/Tune';
import UnarchiveIcon from '@material-ui/icons/Unarchive';
import PowerSettingsNewIcon from '@material-ui/icons/PowerSettingsNew';
import DeleteForeverIcon from '@material-ui/icons/DeleteForever';
import Switch from '@material-ui/core/Switch';
import OpenInNewIcon from '@material-ui/icons/OpenInNew';
import IconButton from '@material-ui/core/IconButton';

import { SETTINGS_COMPONENT_TYPES } from 'const';
import DeleteConfirmDialogContent from './DeleteConfirmDialogContent';
import BrokerNodesPage from './pages/BrokerNodesPage';
import TopicPage from './pages/TopicPage';
import WorkerNodesPage from './pages/WorkerNodesPage';
import WorkspaceFilesPage from './pages/WorkspaceFilesPage';
import WorkspaceNodesPage from './pages/WorkspaceNodesPage';

/*
    Available props for component
      1. SETTINGS_COMPONENT_TYPES.PAGE:
          - title
          - type
          - icon: optional
          - subTitle: optional
          - componentProps: 
            - children: a valid react children

      2. SETTINGS_COMPONENT_TYPES.DIALOG:
          - title
          - type
          - icon: optional
          - subTitle: optional
          - componentProps: all props that are available in Dialog component

      3. SETTINGS_COMPONENT_TYPES.CUSTOMIZED:
          - title
          - type
          - icon: optional
          - subTitle: optional
          - componentProps: 
            - handleClick: optional
            - children: a valid react children
*/

export const useConfig = () => {
  const [workspaceName, setWorkspaceName] = React.useState('');

  const menu = [
    {
      items: [
        {
          icon: <SubjectIcon />,
          text: 'Topics',
        },
        {
          icon: <AssignmentIcon />,
          text: 'Autofill',
        },
      ],
    },
    {
      subHeader: 'services',
      items: [
        {
          icon: <VpnKeyIcon />,
          text: 'Zookeeper',
        },
        {
          icon: <LibraryBooksIcon />,
          text: 'Broker',
        },
        {
          icon: <SettingsInputHdmiIcon />,
          text: 'Worker',
        },
        {
          icon: <WavesIcon />,
          text: 'Stream',
        },
      ],
    },
    {
      subHeader: 'Advanced',
      items: [
        {
          icon: <StorageIcon />,
          text: 'Nodes',
        },
        {
          icon: <FolderOpenIcon />,
          text: 'Files',
        },
        {
          icon: <WarningIcon />,
          text: 'Danger Zone',
        },
      ],
    },
  ];

  const sections = [
    {
      heading: 'Topics',
      components: [
        {
          icon: <SubjectIcon />,
          title: 'Topics in this workspace',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
          componentProps: {
            children: <TopicPage />,
          },
        },
        {
          subTitle: 'Try toggling me',
          title: 'A switch component...',
          type: SETTINGS_COMPONENT_TYPES.CUSTOMIZED,
          componentProps: {
            children: (
              <Switch
                defaultChecked
                color="primary"
                size="small"
                inputProps={{ 'aria-label': 'checkbox with default color' }}
              />
            ),
          },
        },
        {
          icon: <SubjectIcon />,
          title: 'Open a new link',
          type: SETTINGS_COMPONENT_TYPES.CUSTOMIZED,
          componentProps: {
            children: (
              <IconButton
                size="small"
                onClick={() => {
                  window.open('https://www.google.com', '_blank');
                  window.focus();
                }}
              >
                <OpenInNewIcon fontSize="small" />
              </IconButton>
            ),
          },
        },
      ],
    },
    {
      heading: 'Autofill',
      components: [
        {
          icon: <TuneIcon />,
          title: 'Component settings',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
        },
      ],
    },
    {
      heading: 'Zookeeper',
      components: [
        // Feature is disabled because it's not implemented in 0.10
        // {
        //   icon: <TuneIcon />,
        //   title: 'Zookeeper settings',
        //   type: SETTINGS_COMPONENT_TYPES.PAGE,
        // },
        {
          icon: <StorageIcon />,
          title: 'Nodes running the zookeeper',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
        },
      ],
    },
    {
      heading: 'Broker',
      components: [
        // Feature is disabled because it's not implemented in 0.10
        // {
        //   icon: <TuneIcon />,
        //   title: 'Broker settings',
        //   type: SETTINGS_COMPONENT_TYPES.PAGE,
        // },
        {
          icon: <StorageIcon />,
          title: 'Nodes running the broker',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
          componentProps: {
            children: <BrokerNodesPage />,
          },
        },
      ],
    },
    {
      heading: 'Worker',
      components: [
        // Feature is disabled because it's not implemented in 0.10
        // {
        //   icon: <TuneIcon />,
        //   title: 'Worker settings',
        //   type: SETTINGS_COMPONENT_TYPES.PAGE,
        // },
        {
          icon: <StorageIcon />,
          title: 'Nodes running the worker',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
          componentProps: {
            children: <WorkerNodesPage />,
          },
        },
        {
          icon: <UnarchiveIcon />,
          title: 'Plugin jars and Shared jars',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
        },
      ],
    },
    {
      heading: 'Stream',
      components: [
        {
          icon: <UnarchiveIcon />,
          title: 'Stream jars',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
        },
      ],
    },
    {
      heading: 'Nodes',
      components: [
        {
          icon: <StorageIcon />,
          title: 'Nodes in this workspace',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
          componentProps: {
            children: <WorkspaceNodesPage />,
          },
        },
      ],
    },
    {
      heading: 'Files',
      components: [
        {
          icon: <FolderOpenIcon />,
          title: 'Files in this workspace',
          type: SETTINGS_COMPONENT_TYPES.PAGE,
          componentProps: {
            children: <WorkspaceFilesPage />,
          },
        },
      ],
    },
    {
      heading: 'Danger Zone',
      components: [
        // Feature is disabled because it's not implemented in 0.10
        // {
        //   icon: <PowerSettingsNewIcon />,
        //   title: 'Restart this worker',
        //   type: SETTINGS_COMPONENT_TYPES.DIALOG,
        //   componentProps: {
        //     children: 'This will restart the worker.',
        //     title: 'Are you absolutely sure?',
        //     confirmText: 'Restart',
        //     handleConfirm: () => {},
        //   },
        // },
        // {
        //   icon: <PowerSettingsNewIcon />,
        //   title: 'Restart this broker',
        //   type: SETTINGS_COMPONENT_TYPES.DIALOG,
        //   componentProps: {
        //     children: 'This will restart the broker and worker.',
        //     title: 'Are you absolutely sure?',
        //     confirmText: 'Restart',
        //     handleConfirm: () => {},
        //   },
        // },
        {
          icon: <PowerSettingsNewIcon />,
          title: 'Restart this workspace',
          type: SETTINGS_COMPONENT_TYPES.DIALOG,
          componentProps: {
            children: 'This will restart the zookeeper, broker and worker.',
            title: 'Are you absolutely sure?',
            confirmText: 'Restart',
            handleConfirm: () => {},
          },
        },
        {
          icon: <DeleteForeverIcon />,
          title: 'Delete this workspace',
          type: SETTINGS_COMPONENT_TYPES.DIALOG,
          componentProps: {
            children: (
              <DeleteConfirmDialogContent
                workspaceName={workspaceName}
                setWorkspaceName={setWorkspaceName}
              />
            ),
            title: 'Are you absolutely sure?',
            confirmDisabled: !workspaceName,
            confirmText: 'Delete',
            handleConfirm: () => {},
          },
        },
      ],
    },
  ];

  return createRefs(menu, sections);
};

function createRefs(menu, sections) {
  const sectionRefs = {};

  // Add a React ref object in each item
  const sectionsWithRefs = sections.map(section => {
    const key = section.heading;
    sectionRefs[key] = React.createRef();

    return {
      ...section,
      ref: sectionRefs[key],
    };
  });

  const menuWithRefs = menu.map(section => {
    return {
      ...section,
      items: section.items.map(item => {
        return {
          ...item,
          ref: sectionRefs[item.text],
        };
      }),
    };
  });

  return {
    menu: menuWithRefs,
    sections: sectionsWithRefs,
  };
}
