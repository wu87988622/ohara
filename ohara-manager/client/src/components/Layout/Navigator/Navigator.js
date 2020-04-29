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
import PropTypes from 'prop-types';
import clx from 'classnames';
import MenuItem from '@material-ui/core/MenuItem';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import AddIcon from '@material-ui/icons/Add';
import ShareIcon from '@material-ui/icons/Share';
import Menu from '@material-ui/core/Menu';
import Typography from '@material-ui/core/Typography';
import ExtensionIcon from '@material-ui/icons/Extension';
import Scrollbar from 'react-scrollbars-custom';
import { Form, Field } from 'react-final-form';
import Link from '@material-ui/core/Link';

import * as context from 'context';
import * as hooks from 'hooks';
import * as validate from 'utils/validate';
import Outline from './Outline';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import WorkspaceSettings from 'components/Workspace/Settings';
import {
  StyledNavigator,
  StyledButton,
  StyledExpansionPanel,
  PipelineList,
  StyledOutlineList,
} from './NavigatorStyles';

const Navigator = ({ pipelineApi }) => {
  const [anchorEl, setAnchorEl] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const [isExpanded, setIsExpanded] = useState(true);
  const currentWorkspace = hooks.useWorkspace();
  const { open: openEditWorkspaceDialog } = context.useEditWorkspaceDialog();
  const pipelines = hooks.usePipelines();
  const createPipeline = hooks.useCreatePipelineAction();
  const currentPipeline = hooks.usePipeline();
  const switchPipeline = hooks.useSwitchPipelineAction();

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleMenuItemClick = pageName => () => {
    openEditWorkspaceDialog(pageName);
    handleClose();
  };

  const onSubmit = ({ pipelineName: name }, form) => {
    createPipeline({ name });
    setTimeout(form.reset);
    setIsOpen(false);
  };

  if (!currentWorkspace) return null;

  const { name: workspaceName } = currentWorkspace;

  return (
    <StyledNavigator>
      <div className="button-wrapper">
        <StyledButton disableRipple onClick={handleClick}>
          <span className="menu-name">{workspaceName}</span>
          <ExpandMoreIcon />
        </StyledButton>
      </div>
      <Menu
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem
          className="settings"
          key={'settings'}
          onClick={handleMenuItemClick('settings')}
        >
          Settings
        </MenuItem>
      </Menu>

      <Form
        onSubmit={onSubmit}
        initialValues={{}}
        render={({ handleSubmit, form, submitting, pristine, invalid }) => (
          <Dialog
            open={isOpen}
            title="Add a new pipeline"
            onClose={() => {
              setIsOpen(false);
              form.reset();
            }}
            onConfirm={handleSubmit}
            confirmDisabled={submitting || pristine || invalid}
            maxWidth="xs"
            testId="new-pipeline-dialog"
          >
            <form onSubmit={handleSubmit}>
              <Field
                type="text"
                name="pipelineName"
                label="Pipeline name"
                placeholder="pipeline1"
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
            // Only toggles the panel with button not the whole div.
            // This prevents users from accidentally clicking
            // on the div when they're trying to click on the `+` icon in
            // order to create a new pipeline.

            const { nodeName, className } = event.target;
            const isSvg = nodeName === 'svg' || nodeName === 'path';

            // SVG elements also have `className`, but it's not a string 😳
            const isIcon =
              typeof className.includes === 'function' &&
              className.includes('MuiExpansionPanelSummary-expandIcon');

            if (isSvg || isIcon) setIsExpanded(!isExpanded);
          }}
          expandIcon={<ExpandMoreIcon />}
        >
          <Typography variant="h5">Pipelines</Typography>
          <AddIcon
            className="new-pipeline-button"
            onClick={event => {
              event.stopPropagation();
              setIsOpen(true);
            }}
          />
        </ExpansionPanelSummary>
        <ExpansionPanelDetails>
          <div className="scrollbar-wrapper">
            <Scrollbar>
              <PipelineList>
                {pipelines.map(pipeline => (
                  <li key={pipeline.name}>
                    <Link
                      className={clx({
                        'active-link': pipeline.name === currentPipeline?.name,
                      })}
                      onClick={() => {
                        if (pipeline.name !== currentPipeline?.name) {
                          switchPipeline(pipeline.name);
                        }
                      }}
                    >
                      <ShareIcon className="link-icon" />
                      {pipeline.name}
                    </Link>
                  </li>
                ))}
              </PipelineList>
            </Scrollbar>
          </div>
        </ExpansionPanelDetails>
      </StyledExpansionPanel>

      <StyledOutlineList style={{ height: isExpanded ? '50%' : '100%' }}>
        <Typography variant="h5">
          <ExtensionIcon />
          Outline
        </Typography>
        <div className="scrollbar-wrapper">
          <Outline pipelineApi={pipelineApi} />
        </div>
      </StyledOutlineList>
      <WorkspaceSettings />
    </StyledNavigator>
  );
};

Navigator.propTypes = {
  pipelineApi: PropTypes.object,
};

export default Navigator;
