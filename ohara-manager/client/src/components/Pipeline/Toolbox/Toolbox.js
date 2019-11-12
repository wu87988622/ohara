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

import React, { useState, useEffect, useCallback } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import Draggable from 'react-draggable';
import Typography from '@material-ui/core/Typography';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import SearchIcon from '@material-ui/icons/Search';
import AddIcon from '@material-ui/icons/Add';
import CloseIcon from '@material-ui/icons/Close';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import StorageIcon from '@material-ui/icons/Storage';
import { useParams } from 'react-router-dom';
import { isEmpty } from 'lodash';

import * as fileApi from 'api/fileApi';
import { StyledToolbox } from './Styles';
import { useWorkspace } from 'context/WorkspaceContext';
import { useSnackbar } from 'context/SnackbarContext';
import { useAddTopic } from 'context/AddTopicContext';
import { useTopic } from 'context/TopicContext';
import { Label } from 'components/common/Form';
import { AddTopicDialog } from 'components/Topic';

export const StyledInputFile = styled.input`
  display: none;
`;
export const StyledLabel = styled(Label)`
  cursor: pointer;
  height: 24px;
`;

const Toolbox = props => {
  const { isOpen: isToolboxOpen, expanded, handleClose, handleClick } = props;
  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const [streamJars, setStreamJars] = useState([]);
  const { topics, doFetch: fetchTopics } = useTopic();
  const showMessage = useSnackbar();
  const { setIsOpen: setIsAddTopicOpen } = useAddTopic();

  const currentWorkspace = findByWorkspaceName(workspaceName);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace.settings.name, currentWorkspace]);

  const fetchStreamJars = useCallback(async workspaceName => {
    const files = await fileApi.getAll({ group: workspaceName });
    const sortedFiles = files.sort((a, b) => a.name.localeCompare(b.name));

    setStreamJars(sortedFiles);
  }, []);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchStreamJars(currentWorkspace.settings.name);
  }, [currentWorkspace, fetchStreamJars]);

  let sources = [];
  let sinks = [];

  if (currentWorkspace) {
    currentWorkspace.connectorDefinitions.forEach(connectorDef => {
      const kind = connectorDef.settingDefinitions.find(
        settingsDef => settingsDef.key === 'kind',
      );

      const className = connectorDef.className.split('.').pop();

      if (kind.defaultValue === 'source') {
        sources.push(className);
        return;
      }

      sinks.push(className);
    });
  }

  const uploadJar = async file => {
    const response = await fileApi.create({
      file,
      group: workspaceName,
      tags: { type: 'streamjar' },
    });

    const isSuccess = !isEmpty(response);
    if (isSuccess) {
      showMessage('Successfully uploaded the jar');
    }
    fetchStreamJars(currentWorkspace.settings.name);
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];

    const isDuplicate = () => streamJars.some(jar => jar.name === file.name);

    if (isDuplicate()) {
      return showMessage('The jar name is already taken!');
    }

    if (event.target.files[0]) {
      uploadJar(file);
    }
  };

  return (
    <Draggable bounds="parent" handle=".box-title">
      <StyledToolbox className={`${isToolboxOpen ? 'is-open' : ''}`}>
        <div className="title box-title">
          <Typography variant="subtitle1">Toolbox</Typography>
          <IconButton onClick={handleClose}>
            <CloseIcon />
          </IconButton>
        </div>
        <IconButton>
          <SearchIcon />
        </IconButton>
        <InputBase placeholder="Search topic & connector..." />

        <ExpansionPanel square expanded={expanded.source}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('source')}
          >
            <Typography variant="subtitle1">Source</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <List disablePadding>
              {sources.map(source => (
                <ListItem key={source} button dense>
                  <ListItemIcon>
                    <FlightTakeoffIcon />
                  </ListItemIcon>
                  <ListItemText primary={source} />
                </ListItem>
              ))}
            </List>

            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add source connectors</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>

        <ExpansionPanel square expanded={expanded.topic}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('topic')}
          >
            <Typography variant="subtitle1">Topic</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <List disablePadding>
              {topics.map(topic => (
                <ListItem key={topic.settings.name} button dense>
                  <ListItemIcon>
                    <StorageIcon />
                  </ListItemIcon>
                  <ListItemText primary={topic.settings.name} />
                </ListItem>
              ))}
            </List>

            <div className="add-button">
              <IconButton onClick={() => setIsAddTopicOpen(true)}>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add topics</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>

        <AddTopicDialog />

        <ExpansionPanel square expanded={expanded.streamApp}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('streamApp')}
          >
            <Typography variant="subtitle1">StreamApp</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <List disablePadding>
              {streamJars.map(jar => (
                <ListItem key={jar.name} button dense>
                  <ListItemIcon>
                    <StorageIcon />
                  </ListItemIcon>
                  <ListItemText primary={jar.name} />
                </ListItem>
              ))}
            </List>

            <div className="add-button">
              <StyledInputFile
                id="fileInput"
                accept=".jar"
                type="file"
                onChange={handleFileSelect}
                onClick={event => {
                  /* Allow file to be added multiple times */
                  event.target.value = null;
                }}
              />
              <IconButton>
                <StyledLabel htmlFor="fileInput">
                  <AddIcon />
                </StyledLabel>
              </IconButton>

              <Typography variant="subtitle2">Add stream apps</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>

        <ExpansionPanel square expanded={expanded.sink}>
          <ExpansionPanelSummary
            className="panel-title"
            expandIcon={<ExpandMoreIcon />}
            onClick={() => handleClick('sink')}
          >
            <Typography variant="subtitle1">Sink</Typography>
          </ExpansionPanelSummary>
          <ExpansionPanelDetails className="detail">
            <List disablePadding>
              {sinks.map(sink => (
                <ListItem key={sink} button dense>
                  <ListItemIcon>
                    <FlightLandIcon />
                  </ListItemIcon>
                  <ListItemText primary={sink} />
                </ListItem>
              ))}
            </List>

            <div className="add-button">
              <IconButton>
                <AddIcon />
              </IconButton>
              <Typography variant="subtitle2">Add sink connectors</Typography>
            </div>
          </ExpansionPanelDetails>
        </ExpansionPanel>
      </StyledToolbox>
    </Draggable>
  );
};

Toolbox.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  handleClick: PropTypes.func.isRequired,
  expanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    streamApp: PropTypes.bool.isRequired,
  }).isRequired,
};

export default Toolbox;
