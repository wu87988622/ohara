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

import React, { useState } from 'react';
import MenuItem from '@material-ui/core/MenuItem';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import AddIcon from '@material-ui/icons/Add';
import ShareIcon from '@material-ui/icons/Share';
import Menu from '@material-ui/core/Menu';
import { NavLink } from 'react-router-dom';
import { Form, Field } from 'react-final-form';

import * as context from 'context';
import * as validate from 'utils/validate';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import {
  EditWorkspace,
  Tabs as EditWorkspaceTabs,
} from 'components/Workspace/Edit';
import {
  StyledNavigator,
  StyledButton,
  StyledExpansionPanel,
  StyledSubtitle1,
  PipelineList,
} from './NavigatorStyles';

const Navigator = () => {
  const [anchorEl, setAnchorEl] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const { currentWorkspace } = context.useWorkspace();
  const [isExpanded, setIsExpanded] = useState(true);
  const { open: openEditWorkspaceDialog } = context.useEditWorkspaceDialog();
  const { data: pipelines } = context.usePipelineState();
  const { createPipeline } = context.usePipelineActions();

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMenuItemClick = tab => () => {
    openEditWorkspaceDialog({ tab });
    handleClose();
  };

  const onSubmit = async ({ pipelineName: name }, form) => {
    await createPipeline({ name });
    setTimeout(form.reset);
    setIsOpen(false);
  };

  if (!currentWorkspace) return null;

  const {
    settings: { name: workspaceName },
  } = currentWorkspace;

  return (
    <StyledNavigator>
      <StyledButton disableRipple onClick={handleClick}>
        <span className="menu-name">{workspaceName}</span>
        <ExpandMoreIcon />
      </StyledButton>
      <Menu
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        {/* Feature is disabled because it's not implemented in 0.9 */
        false && (
          <MenuItem
            className="overview"
            key={EditWorkspaceTabs.OVERVIEW}
            onClick={handleMenuItemClick(EditWorkspaceTabs.OVERVIEW)}
          >
            Overview
          </MenuItem>
        )}

        <MenuItem
          className="topics"
          key={EditWorkspaceTabs.TOPICS}
          onClick={handleMenuItemClick(EditWorkspaceTabs.TOPICS)}
        >
          Topics
        </MenuItem>
        <MenuItem
          className="files"
          key={EditWorkspaceTabs.FILES}
          onClick={handleMenuItemClick(EditWorkspaceTabs.FILES)}
        >
          Files
        </MenuItem>

        {/* Feature is disabled because it's not implemented in 0.9 */

        false && (
          <MenuItem
            className="settings"
            key={EditWorkspaceTabs.SETTINGS}
            onClick={handleMenuItemClick(EditWorkspaceTabs.SETTINGS)}
          >
            Settings
          </MenuItem>
        )}
      </Menu>

      <Form
        onSubmit={onSubmit}
        initialValues={{}}
        render={({ handleSubmit, form, submitting, pristine, invalid }) => (
          <Dialog
            open={isOpen}
            title="Add a new pipeline"
            handleClose={() => {
              setIsOpen(false);
              form.reset();
            }}
            handleConfirm={handleSubmit}
            confirmDisabled={submitting || pristine || invalid}
          >
            <form onSubmit={handleSubmit}>
              <Field
                type="text"
                name="pipelineName"
                label="Pipeline name"
                placeholder="pipelinename"
                component={InputField}
                autoFocus
                required
                validate={validate.composeValidators(
                  validate.required,
                  validate.minLength(2),
                  validate.maxLength(20),
                  validate.validServiceName,
                )}
              />
            </form>
          </Dialog>
        )}
      />

      <StyledExpansionPanel defaultExpanded={true} expanded={isExpanded}>
        <ExpansionPanelSummary
          disableRipple
          onClick={event => {
            // Only toggles the panel with button (which has the role attr)
            // not the whole div. This prevents users accidentally clicking
            // on the div when they're trying to click on the `+` icon in
            // order to create a new pipeline.
            if (event.target.getAttribute('role')) {
              setIsExpanded(!isExpanded);
            }
          }}
          expandIcon={<ExpandMoreIcon />}
        >
          <StyledSubtitle1>Pipelines</StyledSubtitle1>
          <AddIcon
            className="new-pipeline-button"
            onClick={event => {
              event.stopPropagation();
              setIsOpen(true);
            }}
          />
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <PipelineList>
            {pipelines.map(pipeline => (
              <li key={pipeline.name}>
                <NavLink
                  activeClassName="active-link"
                  to={`/${workspaceName}/${pipeline.name}`}
                >
                  <ShareIcon className="link-icon" />
                  {pipeline.name}
                </NavLink>
              </li>
            ))}
          </PipelineList>
        </ExpansionPanelDetails>
      </StyledExpansionPanel>
      <EditWorkspace />
    </StyledNavigator>
  );
};

export default Navigator;
