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

import React, { useEffect, useRef, createContext } from 'react';
import _ from 'lodash';

import * as context from 'context';
import * as hooks from 'hooks';
import * as pipelineUtils from './PipelineApiHelper';
import Paper from './Paper';
import Toolbar from './Toolbar';
import Toolbox from './Toolbox';
import PipelinePropertyView from './PipelinePropertyView';
import PipelinePropertyDialog from './PipelinePropertyDialog';
import { usePrevious } from 'utils/hooks';
import { PaperWrapper, StyledSplitPane } from './PipelineStyles';
import { usePipelineState as usePipelineReducerState } from './PipelineHooks';
import { CONNECTION_TYPE } from './PipelineApiHelper';
import { KIND, PACKAGE_ROOT } from 'const';
import { DeleteDialog } from 'components/common/Dialog';

export const PaperContext = createContext(null);
export const PipelineStateContext = createContext(null);
export const PipelineDispatchContext = createContext(null);

const Pipeline = React.forwardRef((props, ref) => {
  const currentWorkspace = hooks.useWorkspace();
  const currentPipeline = hooks.usePipeline();
  const setSelectedCell = hooks.useSetSelectedCellAction();
  const startUpdateMetrics = hooks.useStartUpdateMetricsAction();
  const selectedCell = hooks.useCurrentPipelineCell();
  const streams = hooks.useStreams();
  const isStreamLoaded = hooks.useIsStreamLoaded();
  const isConnectorLoaded = hooks.useIsConnectorLoaded();
  const connectors = [...hooks.useConnectors(), ...hooks.useShabondis()];

  const {
    open: openPropertyDialog,
    isOpen: isPropertyDialogOpen,
    close: closePropertyDialog,
    data: propertyDialogData,
  } = context.usePipelinePropertyDialog();

  const [pipelineState, pipelineDispatch] = usePipelineReducerState();

  const {
    create: createConnector,
    update: updateConnector,
    start: startConnector,
    stop: stopConnector,
    remove: removeConnector,
    updateLink: updateLinkConnector,
    removeSourceLink: removeSourceLinkConnector,
    removeSinkLink: removeSinkLinkConnector,
  } = pipelineUtils.connector();

  const {
    create: createStream,
    update: updateStream,
    updateLinkTo: updateStreamLinkTo,
    updateLinkFrom: updateStreamLinkFrom,
    start: startStream,
    stop: stopStream,
    remove: removeStream,
    removeLinkTo: removeStreamLinkTo,
    removeLinkFrom: removeStreamLinkFrom,
  } = pipelineUtils.stream();

  const {
    createAndStart: createAndStartTopic,
    stopAndRemove: stopAndRemoveTopic,
  } = pipelineUtils.topic();

  const { updateCells, getUpdatedCells } = pipelineUtils.pipeline();

  const isInitialized = React.useRef(false);
  const forceDeleteList = React.useRef({});
  const [isOpen, setIsOpen] = React.useState(false);
  const [currentCellData, setCurrentCellData] = React.useState(null);

  const handleDialogClose = () => {
    setIsOpen(false);
    forceDeleteList.current = {};
  };

  const clean = (cells, paperApi, onlyRemoveLink = false) => {
    Object.keys(cells).forEach(key => {
      switch (key) {
        case 'source':
          removeSourceLinkConnector({ ...cells[key] }, cells.topic, paperApi);
          break;
        case 'sink':
          removeSinkLinkConnector({ ...cells[key] }, cells.topic, paperApi);
          break;
        case 'to':
          removeStreamLinkTo({ ...cells[key] }, cells.topic, paperApi);
          break;
        case 'from':
          removeStreamLinkFrom({ ...cells[key] }, cells.topic, paperApi);
          break;
        case 'topic':
          if (onlyRemoveLink) break;
          stopAndRemoveTopic({ ...cells[key] }, paperApi);
          break;

        default:
          break;
      }
    });
  };

  const paperApiRef = useRef(null);
  const pipelineObjectsRef = useRef([]);

  const prevPipeline = usePrevious(currentPipeline);
  useEffect(() => {
    if (prevPipeline?.name === currentPipeline?.name) return;

    // Allow the Paper to reload again
    isInitialized.current = false;

    // re-renders Toolbox
    pipelineDispatch({ type: 'resetToolbox' });
    pipelineDispatch({ type: 'setToolboxKey' });
  }, [currentPipeline, pipelineDispatch, prevPipeline]);

  // When users create a new workspace and there has no pipeline yet
  // we need to clean up the Paper state from previous workspace
  useEffect(() => {
    if (currentPipeline?.name) return;
    if (!paperApiRef.current) return;

    paperApiRef.current.clearGraph({ skipGraphEvents: true });
  }, [currentPipeline]);

  // Only run this once since we only want to load the graph once and maintain
  // the local state ever since
  useEffect(() => {
    if (isInitialized.current) return;
    if (!paperApiRef.current) return;
    if (!isConnectorLoaded) return;
    if (!isStreamLoaded) return;
    if (!currentPipeline) return;

    paperApiRef.current.loadGraph(getUpdatedCells(currentPipeline));

    // Fire this epic only once and it will keep fetching and updating metrics
    // before users leave our App by closing the Tab or browser
    startUpdateMetrics(currentPipeline.name, {
      paperApi: paperApiRef.current,
      pipelineObjectsRef,
    });
    isInitialized.current = true;
  }, [
    isConnectorLoaded,
    currentPipeline,
    getUpdatedCells,
    isStreamLoaded,
    startUpdateMetrics,
  ]);

  const handleSubmit = (params, values, paperApi) => {
    const { cell, topics = [] } = params;
    const { kind } = cell;
    switch (kind) {
      case KIND.source:
      case KIND.sink:
        updateConnector(cell, topics, values, connectors, paperApi);
        break;
      case KIND.stream:
        updateStream(cell, topics, values, streams, paperApi);
        break;
      default:
        break;
    }
  };

  const deleteTopic = () => {
    const paperApi = paperApiRef.current;

    const {
      connectors,
      toStreams,
      fromStreams,
      topic,
    } = forceDeleteList.current;

    connectors
      .filter(connector => _.get(connector, 'state', null) === 'RUNNING')
      .map(connector => {
        const connectorCell = paperApi.getCell(connector.name);
        return stopConnector(connectorCell, paperApi);
      });

    [...toStreams, ...fromStreams]
      .filter(stream => _.get(stream, 'state', null) === 'RUNNING')
      .map(stream => {
        const streamCell = paperApi.getCell(stream.name);
        return stopStream(streamCell, paperApi);
      });

    connectors.forEach(connector => {
      const connectorCell = paperApi.getCell(connector.name);
      const kind = _.get(connectorCell, 'kind');
      // The connectors may be removed by user from UI but not removed from forceDeleteList
      // we should skip those cells which had been removed already
      if (kind) {
        clean({ [kind]: connectorCell, paperApi });
      }
    });

    toStreams.forEach(stream => {
      const streamCell = paperApi.getCell(stream.name);
      clean({ to: streamCell, paperApi });
    });

    fromStreams.forEach(stream => {
      const streamCell = paperApi.getCell(stream.name);
      clean({ from: streamCell, paperApi });
    });

    stopAndRemoveTopic(topic, paperApi);
  };

  const handleCellSelect = element => {
    setSelectedCell(element);
  };

  const handleCellDeselect = () => {
    setSelectedCell(null);
  };

  const handleChange = _.debounce(paperApi => updateCells(paperApi), 1000);

  const handleElementDelete = () => {
    const paperApi = paperApiRef.current;
    const { kind } = currentCellData;

    if (kind === KIND.topic) {
      deleteTopic();
      forceDeleteList.current = {};
    } else if (kind === KIND.source || kind === KIND.sink) {
      removeConnector(currentCellData, paperApi);
    } else if (kind === KIND.stream) {
      removeStream(currentCellData, paperApi);
    }

    setIsOpen(false);
  };

  const getCurrentCellName = cellData => {
    if (cellData === null) return '';
    if (cellData.kind !== KIND.topic) return cellData.name;
    return cellData.isShared ? cellData.name : cellData.displayName;
  };

  const handleElementAdd = (cellData, paperApi) => {
    const { kind, isTemporary } = cellData;
    switch (kind) {
      case KIND.sink:
      case KIND.source:
        if (!isTemporary) {
          createConnector(cellData, paperApi);
        }
        break;

      case KIND.stream:
        if (!isTemporary) {
          createStream(cellData, paperApi);
        }
        break;

      case KIND.topic:
        createAndStartTopic(cellData, paperApi);
        break;

      default:
        break;
    }
  };

  const handleCellConfigClick = (cellData, paperApi) => {
    const { className, displayName, kind, name } = cellData;
    let targetCell;
    switch (kind) {
      case KIND.source:
      case KIND.sink:
        targetCell = connectors.find(connector => connector.name === name);
        break;
      case KIND.stream:
        targetCell = streams.find(stream => stream.name === name);
        break;
      default:
        break;
    }

    const type = className
      .replace(PACKAGE_ROOT, '')
      .split('.')
      .slice(1)
      .shift();
    openPropertyDialog({
      title: `Edit the property of ${displayName} ${kind} ${type}`,
      classInfo: targetCell,
      cellData,
      paperApi,
    });
  };

  const handleConnect = (cells, paperApi) => {
    const {
      type,
      source,
      toStream,
      fromStream,
      sink,
      link,
      topic,
      firstLink,
      secondeLink,
    } = pipelineUtils.utils.getConnectionOrder(cells);
    let sourceRes;
    let sinkRes;
    let toStreamRes;
    let fromStreamRes;
    switch (type) {
      case CONNECTION_TYPE.SOURCE_TOPIC:
        updateLinkConnector({ connector: source, topic, link }, paperApi);
        break;
      case CONNECTION_TYPE.TOPIC_SINK:
        updateLinkConnector({ connector: sink, topic, link }, paperApi);
        break;
      case CONNECTION_TYPE.STREAM_TOPIC:
        updateStreamLinkTo({ toStream, topic, link }, paperApi);
        break;
      case CONNECTION_TYPE.TOPIC_STREAM:
        updateStreamLinkFrom({ fromStream, topic, link }, paperApi);
        break;
      case CONNECTION_TYPE.SOURCE_TOPIC_SINK:
        createAndStartTopic(
          ({
            id: topic.id,
            name: topic.name,
            className: topic.className,
          } = topic),
          paperApi,
        );
        sourceRes = updateLinkConnector(
          { connector: source, topic, link: firstLink },
          paperApi,
        );
        if (sourceRes.error) {
          clean({ topic }, paperApi);
          return;
        }
        sinkRes = updateLinkConnector(
          { connector: sink, topic, link: secondeLink },
          paperApi,
        );
        if (sinkRes.error) {
          clean({ source, topic }, paperApi);
          return;
        }
        break;
      case CONNECTION_TYPE.SOURCE_TOPIC_STREAM:
        createAndStartTopic(
          ({
            id: topic.id,
            name: topic.name,
            className: topic.className,
          } = topic),
          paperApi,
        );
        sourceRes = updateLinkConnector(
          { connector: source, topic, link: firstLink },
          paperApi,
        );
        if (sourceRes.error) {
          clean({ topic }, paperApi);
          return;
        }
        fromStreamRes = updateStreamLinkFrom(
          { fromStream, topic, link },
          paperApi,
        );
        if (fromStreamRes.error) {
          clean({ source, topic }, paperApi);
          return;
        }
        break;
      case CONNECTION_TYPE.STREAM_TOPIC_SINK:
        createAndStartTopic(
          ({
            id: topic.id,
            name: topic.name,
            className: topic.className,
          } = topic),
          paperApi,
        );
        toStreamRes = updateStreamLinkTo({ toStream, topic, link }, paperApi);
        if (toStreamRes.error) {
          clean({ topic }, paperApi);
          return;
        }
        sinkRes = updateLinkConnector(
          { connector: sink, topic, link: secondeLink },
          paperApi,
        );
        if (sinkRes.error) {
          clean({ to: toStream, topic }, paperApi);
          return;
        }
        break;
      case CONNECTION_TYPE.STREAM_TOPIC_STREAM:
        createAndStartTopic(
          ({
            id: topic.id,
            name: topic.name,
            className: topic.className,
          } = topic),
          paperApi,
        );
        toStreamRes = updateStreamLinkTo({ toStream, topic, link }, paperApi);
        if (toStreamRes.error) {
          clean({ topic }, paperApi);
          return;
        }
        fromStreamRes = updateStreamLinkFrom(
          { fromStream, topic, link },
          paperApi,
        );
        if (fromStreamRes.error) {
          clean({ to: toStream, topic }, paperApi);
          return;
        }
        break;
      default:
        break;
    }
  };

  const handleDisConnect = (cells, paperApi) => {
    const {
      type,
      source,
      toStream,
      fromStream,
      sink,
      topic,
    } = pipelineUtils.utils.getConnectionOrder(cells);
    switch (type) {
      case CONNECTION_TYPE.SOURCE_TOPIC:
        clean({ source, topic }, paperApi, true);
        break;
      case CONNECTION_TYPE.STREAM_TOPIC:
        clean({ to: toStream, topic }, paperApi, true);
        break;
      case CONNECTION_TYPE.TOPIC_STREAM:
        clean({ from: fromStream, topic }, paperApi, true);
        break;
      case CONNECTION_TYPE.TOPIC_SINK:
        clean({ sink, topic }, paperApi, true);
        break;
      default:
        break;
    }
  };

  const handleCellStartClick = (cellData, paperApi) => {
    switch (cellData.kind) {
      case KIND.sink:
      case KIND.source:
        startConnector(cellData, paperApi);
        break;

      case KIND.stream:
        startStream(cellData, paperApi);
        break;

      default:
        break;
    }
  };

  const handleCellStopClick = (cellData, paperApi) => {
    switch (cellData.kind) {
      case KIND.sink:
      case KIND.source:
        stopConnector(cellData, paperApi);
        break;

      case KIND.stream:
        stopStream(cellData, paperApi);
        break;

      default:
        break;
    }
  };

  const handleCellRemove = cellData => {
    if (cellData.kind === KIND.topic) {
      const topicConnectors = connectors.filter(connector => {
        const topics =
          _.get(connector, 'topicKeys') ||
          _.get(connector, 'shabondi__source__toTopics') ||
          _.get(connector, 'shabondi__sink__fromTopics') ||
          [];
        return topics.find(topic => topic.name === cellData.name);
      });
      const topicToStream = streams.filter(stream =>
        stream.to.find(topic => topic.name === cellData.name),
      );
      const topicFromStream = streams.filter(stream =>
        stream.from.find(topic => topic.name === cellData.name),
      );

      forceDeleteList.current = {
        connectors: topicConnectors,
        toStreams: topicToStream,
        fromStreams: topicFromStream,
        topic: cellData,
      };

      if (cellData.isShared) {
        // If a shared topic is removed from the Paper, we should
        // re-render the Toolbox, so the topic can be re-added
        // into the Paper again
        pipelineDispatch({ type: 'setToolboxKey' });
      }
    }

    setIsOpen(true);
    setCurrentCellData(cellData);
  };

  // Public APIs
  React.useImperativeHandle(ref, () => {
    if (!paperApiRef.current) return null;

    const paperApi = paperApiRef.current;

    return {
      getElements() {
        return paperApi
          .getCells()
          .filter(
            cell => cell.cellType === 'html.Element' || !cell.isTemporary,
          );
      },

      highlight(id) {
        paperApi.highlight(id);
        const selectedCell = paperApi.getCell(id);
        setSelectedCell(selectedCell);
      },
    };
  });

  return (
    <>
      {currentWorkspace && (
        <PipelineDispatchContext.Provider value={pipelineDispatch}>
          <PipelineStateContext.Provider value={pipelineState}>
            <PaperContext.Provider value={{ ...paperApiRef.current }}>
              {paperApiRef.current && currentPipeline && (
                <Toolbar
                  isToolboxOpen={pipelineState.isToolboxOpen}
                  handleToolboxOpen={() =>
                    pipelineDispatch({ type: 'openToolbox' })
                  }
                  handleToolbarClick={panel => {
                    const activePanel = pipelineState.toolboxExpanded[panel];

                    // Toggle the panel if it's already open
                    if (!activePanel) {
                      pipelineDispatch({ type: 'resetToolbox' });
                    }

                    pipelineDispatch({
                      type: 'setToolbox',
                      payload: panel,
                    });
                  }}
                />
              )}

              <PaperWrapper>
                {selectedCell && (
                  <>
                    <StyledSplitPane
                      split="vertical"
                      minSize={300}
                      maxSize={500}
                      defaultSize={320}
                      primary="second"
                    >
                      <div></div>

                      <PipelinePropertyView
                        handleClose={() => setSelectedCell(null)}
                        element={selectedCell}
                        isMetricsOn={pipelineState.isMetricsOn}
                        pipelineObjects={pipelineObjectsRef.current}
                      />
                    </StyledSplitPane>
                  </>
                )}
                <Paper
                  ref={paperApiRef}
                  onCellSelect={handleCellSelect}
                  onCellDeselect={handleCellDeselect}
                  onChange={handleChange}
                  onElementAdd={handleElementAdd}
                  onConnect={handleConnect}
                  onDisconnect={handleDisConnect}
                  onCellStart={handleCellStartClick}
                  onCellStop={handleCellStopClick}
                  onCellConfig={handleCellConfigClick}
                  onCellRemove={handleCellRemove}
                />
                {paperApiRef.current && currentPipeline && (
                  <Toolbox
                    isOpen={pipelineState.isToolboxOpen}
                    expanded={pipelineState.toolboxExpanded}
                    pipelineDispatch={pipelineDispatch}
                    toolboxKey={pipelineState.toolboxKey}
                  />
                )}
              </PaperWrapper>
              {paperApiRef.current && currentPipeline && (
                <>
                  <PipelinePropertyDialog
                    isOpen={isPropertyDialogOpen}
                    onClose={closePropertyDialog}
                    data={propertyDialogData}
                    onSubmit={handleSubmit}
                  />

                  <DeleteDialog
                    open={isOpen}
                    title="Delete the element"
                    content={`Are you sure you want to delete the element: ${getCurrentCellName(
                      currentCellData,
                    )} ? This action cannot be undone!`}
                    onClose={handleDialogClose}
                    onConfirm={handleElementDelete}
                    maxWidth="xs"
                  />
                </>
              )}
            </PaperContext.Provider>
          </PipelineStateContext.Provider>
        </PipelineDispatchContext.Provider>
      )}
    </>
  );
});

export default Pipeline;
