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

import React, { useState, useEffect, useRef } from 'react';
import PropTypes from 'prop-types';
import Draggable from 'react-draggable';
import Typography from '@material-ui/core/Typography';
import IconButton from '@material-ui/core/IconButton';
import List from '@material-ui/core/List';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import AddIcon from '@material-ui/icons/Add';
import CloseIcon from '@material-ui/icons/Close';
import { useParams } from 'react-router-dom';
import * as joint from 'jointjs';
import * as _ from 'lodash';

import * as fileApi from 'api/fileApi';
import ToolboxAddGraphDialog from './ToolboxAddGraphDialog';
import ToolboxSearch from './ToolboxSearch';
import { StyledToolbox } from './ToolboxStyles';
import {
  useWorkspace,
  useTopicState,
  useTopicActions,
  useAddTopicDialog,
} from 'context';
import { useSnackbar } from 'context/SnackbarContext';
import { Label } from 'components/common/Form';
import { AddTopicDialog } from 'components/Topic';
import { useConnectors, useFiles } from './ToolboxHooks';
import { enableDragAndDrop, createToolboxList } from './toolboxUtils';

const Toolbox = props => {
  const {
    isOpen: isToolboxOpen,
    expanded,
    handleClose,
    handleClick,
    paper,
    graph,
    toolboxKey,
    setToolboxExpanded,
  } = props;

  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const { data: topicsData } = useTopicState();
  const { fetchTopics } = useTopicActions();
  const { open: openAddTopicDialog } = useAddTopicDialog();
  const [isOpen, setIsOpen] = useState(false);
  const [graphType, setGraphType] = useState('');
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [searchResults, setSearchResults] = useState(null);

  const showMessage = useSnackbar();

  const currentWorkspace = findByWorkspaceName(workspaceName);
  const [sources, sinks] = useConnectors(currentWorkspace);
  const { streams, fileNames, setStatus } = useFiles(currentWorkspace);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace.settings.name, currentWorkspace]);

  const privateTopic = {
    settings: { name: 'Pipeline Only' },
  };

  const topics = [privateTopic, ...topicsData].map(topic => ({
    classType: 'topic',
    displayName: topic.settings.name,
  }));

  const uploadJar = async file => {
    const response = await fileApi.create({
      file,
      group: workspaceName,
    });

    const isSuccess = !_.isEmpty(response);
    if (isSuccess) {
      showMessage('Successfully uploaded the jar');
    }
    setStatus('loading');
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];
    const isDuplicate = fileNames.some(fileName => fileName === file.name);

    if (isDuplicate) {
      return showMessage('The jar name is already taken!');
    }

    if (event.target.files[0]) {
      uploadJar(file);
    }
  };

  const handleAddGraph = newGraph => {
    if (newGraph) {
      graph.addCell(
        new joint.shapes.basic.Rect({
          position,
          size: { width: 100, height: 40 },
          attrs: {
            text: { text: newGraph },
            rect: { magnet: true },
          },
        }),
      );
    }

    showMessage(`${newGraph} has been added`);
    setIsOpen(false);
  };

  let sourceGraph = useRef(null);
  let sinkGraph = useRef(null);
  let topicGraph = useRef(null);
  let streamGraph = useRef(null);

  useEffect(() => {
    // Should we handle topic and stream here?
    if (!sources || !sinks) return;

    const renderToolbox = () => {
      const sharedProps = {
        width: 'auto',
        height: 'auto',
        interactive: false,
        // this fixes JointJs cannot properly render these html elements in es6 modules: https://github.com/clientIO/joint/issues/1134
        cellViewNamespace: joint.shapes,
      };

      sourceGraph.current = new joint.dia.Graph();
      topicGraph.current = new joint.dia.Graph();
      streamGraph.current = new joint.dia.Graph();
      sinkGraph.current = new joint.dia.Graph();

      const sourcePaper = new joint.dia.Paper({
        el: document.getElementById('source-list'),
        model: sourceGraph.current,
        ...sharedProps,
      });

      const topicPaper = new joint.dia.Paper({
        el: document.getElementById('topic-list'),
        model: topicGraph.current,
        ...sharedProps,
      });

      const streamPaper = new joint.dia.Paper({
        el: document.getElementById('stream-list'),
        model: streamGraph.current,
        ...sharedProps,
      });

      const sinkPaper = new joint.dia.Paper({
        el: document.getElementById('sink-list'),
        model: sinkGraph.current,
        ...sharedProps,
      });

      createToolboxList({
        sources,
        sinks,
        topics,
        streams,
        streamGraph,
        sourceGraph,
        sinkGraph,
        topicGraph,
        searchResults,
      });

      // Add the ability to drag and drop connectors/streams/topics
      enableDragAndDrop({
        toolPapers: [sourcePaper, sinkPaper, topicPaper, streamPaper],
        paper, // main paper
        setGraphType,
        setPosition,
        setIsOpen,
      });
    };

    renderToolbox();
  }, [paper, searchResults, sinks, sources, streams, topics]);

  return (
    <Draggable bounds="parent" handle=".box-title" key={toolboxKey}>
      <StyledToolbox className={`toolbox ${isToolboxOpen ? 'is-open' : ''}`}>
        <div className="title box-title">
          <Typography variant="subtitle1">Toolbox</Typography>
          <IconButton onClick={handleClose}>
            <CloseIcon />
          </IconButton>
        </div>

        <ToolboxSearch
          searchData={[...sources, ...sinks, ...topics, ...streams]}
          setSearchResults={setSearchResults}
          setToolboxExpanded={setToolboxExpanded}
        />
        <div className="toolbox-body">
          <ExpansionPanel
            square
            expanded={expanded.source}
            defaultExpanded={true}
          >
            <ExpansionPanelSummary
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('source')}
            >
              <Typography variant="subtitle1">Source</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="source-list" className="toolbox-list"></div>
              </List>

              <div className="add-button">
                <IconButton>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">
                  Add source connectors
                </Typography>
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
                <div id="topic-list" className="toolbox-list"></div>
              </List>

              <div className="add-button">
                <IconButton onClick={openAddTopicDialog}>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">Add topics</Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <AddTopicDialog />

          <ExpansionPanel square expanded={expanded.stream}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('stream')}
            >
              <Typography variant="subtitle1">Stream</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="stream-list" className="toolbox-list"></div>
              </List>

              <div className="add-button">
                <input
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
                  <Label htmlFor="fileInput">
                    <AddIcon />
                  </Label>
                </IconButton>

                <Typography variant="subtitle2">Add streams</Typography>
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
                <div id="sink-list" className="toolbox-list"></div>
              </List>

              <div className="add-button">
                <IconButton>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">Add sink connectors</Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>
        </div>

        <ToolboxAddGraphDialog
          isOpen={isOpen}
          graphType={graphType}
          handleConfirm={handleAddGraph}
          handleClose={() => {
            setIsOpen(false);
          }}
        />
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
    stream: PropTypes.bool.isRequired,
  }).isRequired,
  toolboxKey: PropTypes.number.isRequired,
  setToolboxExpanded: PropTypes.func.isRequired,
  paper: PropTypes.any,
  graph: PropTypes.any,
};

export default Toolbox;
