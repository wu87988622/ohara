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
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import WavesIcon from '@material-ui/icons/Waves';
import StorageIcon from '@material-ui/icons/Storage';
import classNames from 'classnames';
import Scrollbar from 'react-scrollbars-custom';
import { Form, Field } from 'react-final-form';
import Link from '@material-ui/core/Link';

import * as context from 'context';
import * as hooks from 'hooks';
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
  PipelineList,
  StyledOutlineList,
} from './NavigatorStyles';
import { KIND } from 'const';
import { AddSharedTopicIcon } from 'components/common/Icon';

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

  const handleMenuItemClick = tab => () => {
    openEditWorkspaceDialog({ tab });
    handleClose();
  };

  const onSubmit = ({ pipelineName: name }, form) => {
    createPipeline({ name });
    // if (!res.error) {
    //   eventLog.info(`Successfully created pipeline ${name}.`);
    // }
    setTimeout(form.reset);
    setIsOpen(false);
  };

  const getIcon = (kind, isShared) => {
    const { source, sink, stream, topic } = KIND;

    if (kind === source) return <FlightTakeoffIcon />;
    if (kind === sink) return <FlightLandIcon />;
    if (kind === stream) return <WavesIcon />;
    if (kind === topic) {
      return isShared ? (
        <AddSharedTopicIcon width={20} height={22} />
      ) : (
        <StorageIcon />
      );
    }
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

        <MenuItem
          className="autofill"
          key={EditWorkspaceTabs.AUTOFILL}
          onClick={handleMenuItemClick(EditWorkspaceTabs.AUTOFILL)}
        >
          Autofill
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
        {pipelineApi && (
          <div className="scrollbar-wrapper">
            <Scrollbar>
              <ul className="list">
                {pipelineApi.getElements().map(element => {
                  const {
                    id,
                    name,
                    kind,
                    isSelected,
                    isShared,
                    displayName,
                  } = element;

                  const isTopic = kind === KIND.topic;

                  const className = classNames({
                    'is-selected': isSelected,
                    'is-shared': isTopic && isShared,
                    'pipeline-only': isTopic && !isShared,
                    [kind]: kind,
                  });

                  return (
                    <li
                      className={className}
                      onClick={() => pipelineApi.highlight(id)}
                      key={id}
                    >
                      {getIcon(kind, isShared)}
                      {isTopic && !isShared ? displayName : name}
                    </li>
                  );
                })}
              </ul>
            </Scrollbar>
          </div>
        )}
      </StyledOutlineList>

      <EditWorkspace />
    </StyledNavigator>
  );
};

Navigator.propTypes = {
  pipelineApi: PropTypes.object,
};

export default Navigator;
