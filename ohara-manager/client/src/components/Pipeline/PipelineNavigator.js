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

import React, { useState, useCallback } from 'react';
import PropTypes from 'prop-types';
import MenuItem from '@material-ui/core/MenuItem';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import Menu from '@material-ui/core/Menu';
import { NavLink, useParams } from 'react-router-dom';
import { get } from 'lodash';
import { Form, Field } from 'react-final-form';

import * as pipelineApi from 'api/pipelineApi';
import { InputField } from 'components/common/Form';
import { Dialog } from 'components/common/Dialog';
import { useSnackbar } from 'context/SnackbarContext';
import {
  required,
  validServiceName,
  lessThanTweenty,
  composeValidators,
} from 'utils/validate';
import {
  Navigator,
  StyledButton,
  StyledExpansionPanel,
  StyledSubtitle1,
  PipelineList,
} from './Styles';

const PipelineNavigator = () => {
  const [pipelines, setPipelines] = useState([]);
  const [anchorEl, setAnchorEl] = useState(null);
  const [isOpen, setIsOpen] = useState(false);
  const { workspaceName } = useParams();
  const showMessage = useSnackbar();

  const handleClick = event => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const fetchPipelines = useCallback(async () => {
    const response = await pipelineApi.getAll({ group: workspaceName });
    const pipelines = get(response, 'data.result', []).sort((a, b) =>
      a.name.localeCompare(b.name),
    );
    setPipelines(pipelines);
  }, [workspaceName]);

  const onSubmit = async ({ pipelineName }, form) => {
    // TODO: we should get rid of thie error handling logic
    // once #2995 is merged
    try {
      const response = await pipelineApi.create({
        name: pipelineName,
        group: workspaceName,
      });
      if (response.data.isSuccess) {
        showMessage(`Pipeline ${pipelineName} has been added`);
        await fetchPipelines();
        setTimeout(form.reset);
      }
    } catch (error) {
      showMessage(error.message);
    }

    setIsOpen(false);
  };

  React.useEffect(() => {
    fetchPipelines();
  }, [fetchPipelines]);

  return (
    <Navigator>
      <StyledButton disableRipple onClick={handleClick}>
        <span className="menu-name">{workspaceName}</span>
        <i className="fas fa-angle-down" />
      </StyledButton>
      <Menu
        anchorEl={anchorEl}
        keepMounted
        open={Boolean(anchorEl)}
        onClose={handleClose}
      >
        <MenuItem onClick={handleClose}>Node settings</MenuItem>
        <MenuItem onClick={handleClose}>Worker settings</MenuItem>
        <MenuItem onClick={handleClose}>Broker settings</MenuItem>
        <MenuItem onClick={handleClose}>Zookeeper settings</MenuItem>
      </Menu>

      <Form
        onSubmit={onSubmit}
        initialValues={{}}
        render={({ handleSubmit, form, pristine, invalid }) => (
          <Dialog
            open={isOpen}
            title="Add a new pipeline"
            handleClose={() => {
              setIsOpen(false);
              form.reset();
            }}
            handleConfirm={handleSubmit}
            confirmDisabled={pristine || invalid}
          >
            <form onSubmit={handleSubmit}>
              <Field
                name="pipelineName"
                type="text"
                label="Pipeline name"
                placeholder="pipelinename"
                validate={composeValidators(
                  required,
                  lessThanTweenty,
                  validServiceName,
                )}
                component={InputField}
                autoFocus
                required
              />
            </form>
          </Dialog>
        )}
      />

      <StyledExpansionPanel defaultExpanded={true}>
        <ExpansionPanelSummary
          disableRipple
          expandIcon={<i className="fas fa-angle-down" />}
        >
          <StyledSubtitle1>Pipelines</StyledSubtitle1>
          <i
            className="new-pipeline-button fas fa-plus"
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
                  <i className="fas fa-project-diagram"></i>
                  {pipeline.name}
                </NavLink>
              </li>
            ))}
          </PipelineList>
        </ExpansionPanelDetails>
      </StyledExpansionPanel>
    </Navigator>
  );
};

PipelineNavigator.propTypes = {
  prop: PropTypes.any,
};

export default PipelineNavigator;
