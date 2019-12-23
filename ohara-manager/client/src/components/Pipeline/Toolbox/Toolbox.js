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
import * as joint from 'jointjs';

import * as fileApi from 'api/fileApi';
import * as connectorApi from 'api/connectorApi';
import * as streamApi from 'api/streamApi';
import * as context from 'context';
import ToolboxAddGraphDialog from './ToolboxAddGraphDialog';
import ToolboxSearch from './ToolboxSearch';
import { StyledToolbox } from './ToolboxStyles';
import { useSnackbar } from 'context/SnackbarContext';
import { Label } from 'components/common/Form';
import { AddTopicDialog } from 'components/Topic';
import { useFiles, useToolboxHeight } from './ToolboxHooks';
import {
  enableDragAndDrop,
  createToolboxList,
  getConnectorInfo,
} from './toolboxUtils';
import ConnectorGraph from '../Graph/Connector/ConnectorGraph';
import { TopicGraph } from '../Graph/Topic';
import { getKey, hashKey } from 'utils/object';

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
    initToolboxList,
  } = props;

  const {
    currentWorker,
    currentWorkspace,
    currentBroker,
  } = context.useWorkspace();
  const { data: topicsData } = context.useTopicState();
  const { fetchTopics } = context.useTopicActions();
  const { open: openAddTopicDialog } = context.useAddTopicDialog();
  const { open: openSettingDialog, setData } = context.useGraphSettingDialog();
  const [isOpen, setIsOpen] = useState(false);

  const [zIndex, setZIndex] = useState(2);
  const [searchResults, setSearchResults] = useState(null);
  const [cellInfo, setCellInfo] = useState({
    classType: '',
    className: '',
    displayedClassName: '',
    icon: '',
    position: {
      x: 0,
      y: 0,
    },
  });

  const showMessage = useSnackbar();

  const { streams, files: streamFiles, setStatus } = useFiles(currentWorkspace);
  const [sources, sinks] = getConnectorInfo(currentWorker);

  const privateTopic = {
    settings: { name: 'Pipeline Only' },
  };

  const topics = [privateTopic, ...topicsData].map(topic => ({
    classType: 'topic',
    displayName: topic.settings.name,
  }));

  const connectors = {
    sources,
    topics,
    streams,
    sinks,
  };

  const {
    toolboxHeight,
    toolboxRef,
    toolboxHeaderRef,
    panelSummaryRef,
    panelAddButtonRef,
  } = useToolboxHeight({
    expanded,
    paper,
    searchResults,
    connectors,
  });

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace);
  }, [fetchTopics, currentWorkspace]);

  const uploadJar = async file => {
    const response = await fileApi.create({
      file,
      group: hashKey(currentWorkspace),
    });

    showMessage(response.title);
    setStatus('loading');
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];
    const isDuplicate = streamFiles.some(
      streamFile => streamFile.name === file.name,
    );

    if (isDuplicate) {
      return showMessage('The jar name is already taken!');
    }

    if (event.target.files[0]) {
      uploadJar(file);
    }
  };

  const removeTempCell = () => {
    // Remove temporary cells
    const tempCells = graph
      .getCells()
      .filter(cell => Boolean(cell.attributes.isTemporary));
    tempCells.forEach(cell => cell.remove());
  };

  const handleAddGraph = async newGraphName => {
    setZIndex(zIndex + 1);

    const sharedParams = {
      title: newGraphName,
      graph,
      paper,
      cellInfo,
    };

    switch (cellInfo.classType) {
      case 'topic':
        graph.addCell(
          TopicGraph({
            ...sharedParams,
          }),
        );
        break;

      case 'stream':
        const [targetStream] = streamFiles
          .map(streamFile => streamFile.classInfos)
          .flat()
          .filter(infos => infos.className === cellInfo.className);

        const requestParams = {
          group: hashKey(currentWorkspace),
          jarKey: getKey(currentWorkspace),
          brokerClusterKey: getKey(currentBroker),
          connector__class: cellInfo.className,
        };
        const definition = targetStream;

        streamApi.create(requestParams, definition);

        graph.addCell(
          ConnectorGraph({
            ...sharedParams,
          }),
        );

        break;

      case 'source':
      case 'sink':
        const { classInfos } = currentWorker;

        await connectorApi.create({
          classInfos,
          group: hashKey(currentWorkspace),
          workerClusterKey: getKey(currentWorker),
          connector__class: cellInfo.className,
        });

        const [targetConnector] = classInfos.filter(
          classInfo => classInfo.className === cellInfo.className,
        );

        graph.addCell(
          ConnectorGraph({
            ...sharedParams,
            openSettingDialog,
            setData,
            classInfo: targetConnector,
          }),
        );
        break;

      default:
        break;
    }

    removeTempCell();
    showMessage(`${newGraphName} has been added`);
    setIsOpen(false);
  };

  let sourceGraph = useRef(null);
  let sinkGraph = useRef(null);
  let topicGraph = useRef(null);
  let streamGraph = useRef(null);

  useEffect(() => {
    // Should we handle topic and stream here?
    if (!connectors.sources || !connectors.sinks) return;

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
        connectors,
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
        setCellInfo,
        setIsOpen,
        graph,
      });
    };

    renderToolbox();
  }, [connectors, graph, paper, searchResults, initToolboxList]);

  return (
    <Draggable
      bounds="parent"
      handle=".toolbox-title"
      ref={toolboxRef}
      key={toolboxKey}
    >
      <StyledToolbox className={`toolbox ${isToolboxOpen ? 'is-open' : ''}`}>
        <div className="toolbox-header" ref={toolboxHeaderRef}>
          <div className="title toolbox-title">
            <Typography variant="subtitle1">Toolbox</Typography>
            <IconButton onClick={handleClose}>
              <CloseIcon />
            </IconButton>
          </div>

          <ToolboxSearch
            searchData={Object.values(connectors).flat()}
            setSearchResults={setSearchResults}
            setToolboxExpanded={setToolboxExpanded}
          />
        </div>

        <div
          className="toolbox-body"
          style={{ height: toolboxHeight ? toolboxHeight : 'auto' }}
        >
          <ExpansionPanel square expanded={expanded.source}>
            <ExpansionPanelSummary
              ref={panelSummaryRef}
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('source')}
            >
              <Typography variant="subtitle1">Source</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="source-list" className="toolbox-list"></div>
              </List>

              <div className="add-button" ref={panelAddButtonRef}>
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
          classType={cellInfo.classType}
          handleConfirm={handleAddGraph}
          handleClose={() => {
            setIsOpen(false);
            removeTempCell();
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
  initToolboxList: PropTypes.number.isRequired,
  paper: PropTypes.any,
  graph: PropTypes.any,
};

export default Toolbox;
