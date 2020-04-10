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
import Paper from './Paper';
import Toolbar from './Toolbar';
import Toolbox from './Toolbox';
import PipelinePropertyView from './PipelinePropertyView';
import PipelinePropertyDialog from './PipelinePropertyDialog';
import { usePrevious } from 'utils/hooks';
import { PaperWrapper, StyledSplitPane } from './PipelineStyles';
import { usePipelineState as usePipelineReducerState } from './PipelineHooks';
import * as pipelineUtils from './PipelineApiHelper';
import { CONNECTION_TYPE } from './PipelineApiHelper';
import { KIND } from 'const';
import { DeleteDialog } from 'components/common/Dialog';

export const PaperContext = createContext(null);
export const PipelineStateContext = createContext(null);
export const PipelineDispatchContext = createContext(null);

const Pipeline = React.forwardRef((props, ref) => {
  const currentWorkspace = hooks.useWorkspace();
  const currentPipeline = hooks.usePipeline();
  const setSelectedCell = hooks.useSetSelectedCellAction();
  const fetchPipeline = hooks.useFetchPipelineAction();
  const selectedCell = hooks.useCurrentPipelineCell();
  const streams = hooks.useStreams();
  const isStreamLoaded = hooks.useIsStreamLoaded();

  const {
    open: openPropertyDialog,
    isOpen: isPropertyDialogOpen,
    close: closePropertyDialog,
    data: propertyDialogData,
  } = context.usePipelinePropertyDialog();

  const connectorLastUpdated = hooks.useIsConnectorLoaded();
  const connectors = hooks.useConnectors();
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
  const [url, setUrl] = React.useState(null);

  const workspaceName = hooks.useWorkspaceName();
  const pipelineName = hooks.usePipelineName();

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
  const cellsMetricsRef = useRef([]);
  const isPaperApiReady = _.has(paperApiRef, 'current.state.isReady');

  const prevPipeline = usePrevious(currentPipeline);
  // Reset toolbox states
  useEffect(() => {
    const currentPipelineName = _.get(currentPipeline, 'name', '');
    const prevPipelineName = _.get(prevPipeline, 'name', '');
    if (currentPipelineName === prevPipelineName) return;

    pipelineDispatch({ type: 'resetToolbox' });
    // re-renders Toolbox
    pipelineDispatch({ type: 'setToolboxKey' });
  }, [currentPipeline, pipelineDispatch, prevPipeline]);

  // If paper API is not ready, let's reset the pipeline state and re-render again
  useEffect(() => {
    if (!isPaperApiReady && pipelineName) {
      pipelineDispatch({ type: 'resetPipeline' });
    }
  }, [isPaperApiReady, pipelineDispatch, pipelineName]);

  useEffect(() => {
    let timer;
    if (pipelineState.isMetricsOn) {
      if (currentPipeline) {
        timer = setInterval(async () => {
          const res = await fetchPipeline(currentPipeline.name);
          if (isPaperApiReady) {
            const objects = _.get(res, 'data[0].objects', []);
            const nodeMetrics = objects.filter(
              object => object.kind !== KIND.topic,
            );
            // Topic metrics are not displayed in Paper.
            paperApiRef.current.updateMetrics(nodeMetrics);
            cellsMetricsRef.current = objects;
          }
        }, 5000);
      }
    }

    return () => {
      clearInterval(timer);
    };
  });

  useEffect(() => {
    if (!workspaceName || !pipelineName) return;
    if (url !== `${workspaceName}/${pipelineName}`) {
      paperApiRef.current = null;
      isInitialized.current = false;
      setUrl(`${workspaceName}/${pipelineName}`);
    }
  }, [workspaceName, pipelineName, url]);

  useEffect(() => {
    // Only run this once since we only want to load the graph once and maintain
    // the local state ever since

    if (!isPaperApiReady) return;
    if (isInitialized.current) return;
    if (!paperApiRef.current) return;

    if (!connectorLastUpdated) return;
    if (!isStreamLoaded) return;
    if (pipelineName !== _.get(currentPipeline, 'name', null)) return;

    paperApiRef.current.loadGraph(getUpdatedCells(currentPipeline));
    isInitialized.current = true;
  }, [
    connectorLastUpdated,
    currentPipeline,
    getUpdatedCells,
    isPaperApiReady,
    isStreamLoaded,
    pipelineName,
  ]);

  const handleSubmit = (params, values, paperApi) => {
    const { cell, topics = [] } = params;
    const { kind } = cell;
    switch (kind) {
      case KIND.source:
      case KIND.sink:
        updateConnector(cell, topics, values, paperApi);
        break;
      case KIND.stream:
        updateStream(cell, topics, values, streams, paperApi);
        break;
      default:
        break;
    }
  };

  const deleteTopic = async () => {
    const paperApi = paperApiRef.current;

    const {
      connectors,
      toStreams,
      fromStreams,
      topic,
    } = forceDeleteList.current;

    await Promise.all(
      connectors
        .filter(connector => _.get(connector, 'state', null) === 'RUNNING')
        .map(connector => {
          const connectorCell = paperApi.getCell(connector.name);
          return stopConnector(connectorCell, paperApi);
        }),
    );
    await Promise.all(
      [...toStreams, ...fromStreams]
        .filter(stream => _.get(stream, 'state', null) === 'RUNNING')
        .map(stream => {
          const streamCell = paperApi.getCell(stream.name);
          return stopStream(streamCell, paperApi);
        }),
    );

    connectors.forEach(connector => {
      const connectorCell = paperApi.getCell(connector.name);
      const kind = connectorCell.kind;
      clean({ [kind]: connectorCell, paperApi });
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

  const handleElementDelete = async () => {
    const paperApi = paperApiRef.current;
    const { kind } = currentCellData;

    if (kind === KIND.topic) {
      await deleteTopic();
      forceDeleteList.current = {};
    } else if (kind === KIND.source || kind === KIND.sink) {
      await removeConnector(currentCellData, paperApi);
    } else if (kind === KIND.stream) {
      await removeStream(currentCellData, paperApi);
    }

    setIsOpen(false);
  };

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

  const getCurrentCellName = cellData => {
    if (cellData === null) return '';
    if (cellData.kind !== KIND.topic) return cellData.name;
    return cellData.isShared ? cellData.name : cellData.displayName;
  };

  return (
    <>
      {currentWorkspace && currentPipeline && (
        <>
          <PipelineDispatchContext.Provider value={pipelineDispatch}>
            <PipelineStateContext.Provider value={pipelineState}>
              <PaperContext.Provider value={{ ...paperApiRef.current }}>
                {isPaperApiReady && (
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
                          cellsMetrics={cellsMetricsRef.current}
                        />
                      </StyledSplitPane>
                    </>
                  )}
                  <Paper
                    ref={paperApiRef}
                    onCellSelect={element => setSelectedCell(element)}
                    onCellDeselect={() => setSelectedCell(null)}
                    onChange={_.debounce(
                      paperApi => updateCells(paperApi),
                      1000,
                    )}
                    onElementAdd={(cellData, paperApi) => {
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
                    }}
                    onCellConfig={(cellData, paperApi) => {
                      const { displayName, kind, name } = cellData;
                      let targetCell;
                      switch (kind) {
                        case KIND.source:
                        case KIND.sink:
                          targetCell = connectors.find(
                            connector => connector.name === name,
                          );
                          break;
                        case KIND.stream:
                          targetCell = streams.find(
                            stream => stream.name === name,
                          );
                          break;
                        default:
                          break;
                      }
                      openPropertyDialog({
                        title: `Edit the property of ${displayName} ${kind} connector`,
                        classInfo: targetCell,
                        cellData,
                        paperApi,
                      });
                    }}
                    onConnect={async (cells, paperApi) => {
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
                          updateLinkConnector(
                            { connector: source, topic, link },
                            paperApi,
                          );
                          break;
                        case CONNECTION_TYPE.TOPIC_SINK:
                          updateLinkConnector(
                            { connector: sink, topic, link },
                            paperApi,
                          );
                          break;
                        case CONNECTION_TYPE.STREAM_TOPIC:
                          updateStreamLinkTo(
                            { toStream, topic, link },
                            paperApi,
                          );
                          break;
                        case CONNECTION_TYPE.TOPIC_STREAM:
                          updateStreamLinkFrom(
                            { fromStream, topic, link },
                            paperApi,
                          );
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
                          sourceRes = await updateLinkConnector(
                            { connector: source, topic, link: firstLink },
                            paperApi,
                          );
                          if (sourceRes.error) {
                            clean({ topic }, paperApi);
                            return;
                          }
                          sinkRes = await updateLinkConnector(
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
                          sourceRes = await updateLinkConnector(
                            { connector: source, topic, link: firstLink },
                            paperApi,
                          );
                          if (sourceRes.error) {
                            clean({ topic }, paperApi);
                            return;
                          }
                          fromStreamRes = await updateStreamLinkFrom(
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
                          toStreamRes = await updateStreamLinkTo(
                            { toStream, topic, link },
                            paperApi,
                          );
                          if (toStreamRes.error) {
                            clean({ topic }, paperApi);
                            return;
                          }
                          sinkRes = await updateLinkConnector(
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
                          toStreamRes = await updateStreamLinkTo(
                            { toStream, topic, link },
                            paperApi,
                          );
                          if (toStreamRes.error) {
                            clean({ topic }, paperApi);
                            return;
                          }
                          fromStreamRes = await updateStreamLinkFrom(
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
                    }}
                    onDisconnect={(cells, paperApi) => {
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
                    }}
                    onCellStart={(cellData, paperApi) => {
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
                    }}
                    onCellStop={(cellData, paperApi) => {
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
                    }}
                    onCellRemove={async cellData => {
                      if (cellData.kind === KIND.topic) {
                        const topicConnectors = connectors.filter(connector =>
                          connector.topicKeys.find(
                            topic => topic.name === cellData.name,
                          ),
                        );
                        const topicToStream = streams.filter(stream =>
                          stream.to.find(topic => topic.name === cellData.name),
                        );
                        const topicFromStream = streams.filter(stream =>
                          stream.from.find(
                            topic => topic.name === cellData.name,
                          ),
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
                    }}
                  />

                  {isPaperApiReady && (
                    <Toolbox
                      isOpen={pipelineState.isToolboxOpen}
                      expanded={pipelineState.toolboxExpanded}
                      pipelineDispatch={pipelineDispatch}
                      toolboxKey={pipelineState.toolboxKey}
                    />
                  )}
                </PaperWrapper>
                {isPaperApiReady && (
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
                      handleClose={handleDialogClose}
                      handleConfirm={handleElementDelete}
                    />
                  </>
                )}
              </PaperContext.Provider>
            </PipelineStateContext.Provider>
          </PipelineDispatchContext.Provider>
        </>
      )}
    </>
  );
});

export default Pipeline;
