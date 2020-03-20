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

import ToolboxAddGraphDialog from './ToolboxAddGraphDialog';
import ToolboxSearch from './ToolboxSearch';
import ToolboxUploadButton from './ToolboxUploadButton';
import * as utils from './ToolboxUtils';
import * as context from 'context';
import * as hooks from 'hooks';
import { KIND } from 'const';
import { StyledToolbox } from './ToolboxStyles';
import { AddTopicDialog } from 'components/Topic';
import { useFiles, useToolboxHeight, useTopics } from './ToolboxHooks';
import { PaperContext } from '../Pipeline';

const Toolbox = props => {
  const {
    isOpen: isToolboxOpen,
    expanded,
    toolboxKey,
    pipelineDispatch,
  } = props;

  const currentWorker = hooks.useWorker();
  const currentWorkspace = hooks.useWorkspace();
  const currentPipeline = hooks.usePipeline();
  const { createFile } = context.useFileActions();

  const { open: openAddTopicDialog } = context.useAddTopicDialog();
  const showMessage = context.useSnackbar();

  const [isOpen, setIsOpen] = React.useState(false);
  const [searchResults, setSearchResults] = React.useState(null);
  const [cellInfo, setCellInfo] = React.useState({
    kind: '',
    className: '',
    position: {
      displayName: '',
      x: 0,
      y: 0,
    },
  });
  const paperApi = React.useContext(PaperContext);
  const { streams } = useFiles();
  const [sources, sinks] = utils.getConnectorInfo(currentWorker);
  const [topics, topicsData] = useTopics(currentWorkspace);
  const toolboxBodyRef = React.useRef(null);
  const scrollRef = React.useRef(0);

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
    searchResults,
    connectors,
  });

  const handleFileSelect = async event => {
    const [file] = event.target.files;

    if (!file) return;
    const { error } = await createFile(file);
    if (error) return showMessage(error);
  };

  const handleAddGraph = async newName => {
    if (!utils.checkUniqueName(newName, paperApi)) {
      setIsOpen(false);
      utils.removeTemporaryCell(paperApi);
      return showMessage(
        `The name "${newName}" is already taken in this pipeline, please use a different name!`,
      );
    }

    const params = {
      ...cellInfo,
      displayName: newName,
    };

    switch (cellInfo.kind) {
      case KIND.stream:
        paperApi.addElement({
          ...params,
          name: newName,
          displayName: newName,
        });

        break;

      case KIND.source:
      case KIND.sink:
        paperApi.addElement({
          ...params,
          name: newName,
          displayName: newName,
        });
        break;

      default:
    }

    utils.removeTemporaryCell(paperApi);
    setIsOpen(false);
    showMessage(`${newName} has been added`);
  };

  let sourceGraph = React.useRef(null);
  let sinkGraph = React.useRef(null);
  let topicGraph = React.useRef(null);
  let streamGraph = React.useRef(null);

  React.useEffect(() => {
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

      utils.createToolboxList({
        connectors,
        streamGraph,
        sourceGraph,
        sinkGraph,
        topicGraph,
        searchResults,
        paperApi,
      });

      // Add the ability to drag and drop connectors/streams/topics
      utils.enableDragAndDrop({
        toolPapers: [sourcePaper, sinkPaper, topicPaper, streamPaper],
        setCellInfo,
        setIsOpen,
        paperApi,
        showMessage,
      });
    };

    renderToolbox();
  }, [
    connectors,
    currentPipeline,
    paperApi,
    searchResults,
    showMessage,
    topicsData,
  ]);

  const handleScroll = () => {
    const scrollTop = toolboxBodyRef.current.scrollTop;
    scrollRef.current = scrollTop;
  };

  if (toolboxBodyRef.current) {
    if (toolboxBodyRef.current.scrollTop !== scrollRef.current) {
      toolboxBodyRef.current.scrollTop = scrollRef.current;
    }
  }

  return (
    <Draggable
      bounds="parent"
      handle=".toolbox-title"
      ref={toolboxRef}
      key={toolboxKey}
    >
      <StyledToolbox
        className={`toolbox ${isToolboxOpen ? 'is-open' : ''}`}
        data-testid="toolbox-draggable"
      >
        <div className="toolbox-header" ref={toolboxHeaderRef}>
          <div className="title toolbox-title">
            <Typography variant="subtitle1">Toolbox</Typography>
            <IconButton
              onClick={() => pipelineDispatch({ type: 'closeToolbox' })}
            >
              <CloseIcon />
            </IconButton>
          </div>

          <ToolboxSearch
            searchData={Object.values(connectors).reduce(
              (acc, cur) => acc.concat(cur),
              [],
            )}
            setSearchResults={setSearchResults}
            pipelineDispatch={pipelineDispatch}
          />
        </div>

        <div
          className="toolbox-body"
          style={{ height: toolboxHeight ? toolboxHeight : 'auto' }}
          ref={toolboxBodyRef}
          onScroll={handleScroll}
        >
          <ExpansionPanel square expanded={expanded.source}>
            <ExpansionPanelSummary
              ref={panelSummaryRef}
              expandIcon={<ExpandMoreIcon />}
              onClick={() =>
                pipelineDispatch({ type: 'setToolbox', payload: 'source' })
              }
            >
              <Typography variant="subtitle1">Source</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="source-list" className="toolbox-list"></div>
              </List>

              {false && (
                <ToolboxUploadButton
                  buttonText="Add source connectors"
                  onChange={handleFileSelect}
                  ref={panelAddButtonRef}
                />
              )}
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <ExpansionPanel square expanded={expanded.topic}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() =>
                pipelineDispatch({ type: 'setToolbox', payload: 'topic' })
              }
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

          <AddTopicDialog uniqueId="toolbox" />

          <ExpansionPanel square expanded={expanded.stream}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() =>
                pipelineDispatch({ type: 'setToolbox', payload: 'stream' })
              }
            >
              <Typography variant="subtitle1">Stream</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="stream-list" className="toolbox-list"></div>
              </List>

              <ToolboxUploadButton
                buttonText="Add streams"
                onChange={handleFileSelect}
                ref={panelAddButtonRef}
              />
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <ExpansionPanel square expanded={expanded.sink}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() =>
                pipelineDispatch({ type: 'setToolbox', payload: 'sink' })
              }
            >
              <Typography variant="subtitle1">Sink</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="sink-list" className="toolbox-list"></div>
              </List>

              {false && (
                <ToolboxUploadButton
                  buttonText="Add sink connectors"
                  onChange={handleFileSelect}
                  ref={panelAddButtonRef}
                />
              )}
            </ExpansionPanelDetails>
          </ExpansionPanel>
        </div>

        <ToolboxAddGraphDialog
          isOpen={isOpen}
          kind={cellInfo.kind}
          handleConfirm={handleAddGraph}
          handleClose={() => {
            setIsOpen(false);
            utils.removeTemporaryCell(paperApi);
          }}
        />
      </StyledToolbox>
    </Draggable>
  );
};

Toolbox.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  pipelineDispatch: PropTypes.func.isRequired,
  expanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    stream: PropTypes.bool.isRequired,
  }).isRequired,
  toolboxKey: PropTypes.number.isRequired,
};

export default Toolbox;
