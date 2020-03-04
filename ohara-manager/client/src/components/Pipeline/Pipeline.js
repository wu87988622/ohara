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
import { useParams } from 'react-router-dom';

import * as context from 'context';
import Paper from './Paper';
import Toolbar from './Toolbar';
import Toolbox from './Toolbox';
import PipelinePropertyView from './PipelinePropertyView';
import NodeDialog from 'components/Node/NodeDialog';
import IntroDialog from './IntroDialog';
import PipelinePropertyDialog from './PipelinePropertyDialog';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { usePrevious } from 'utils/hooks';
import { PaperWrapper, StyledSplitPane } from './PipelineStyles';
import {
  usePipelineState as usePipelineReducerState,
  useRedirect,
} from './PipelineHooks';
import * as pipelineUtils from './PipelineApiHelper';
import { CONNECTION_TYPE } from './PipelineApiHelper';
import { KIND } from 'const';
import { DeleteDialog } from 'components/common/Dialog';

export const PaperContext = createContext(null);
export const PipelineStateContext = createContext(null);
export const PipelineDispatchContext = createContext(null);

const Pipeline = React.forwardRef((props, ref) => {
  const {
    workspaces,
    currentWorkspace,
    currentPipeline,
    currentWorker,
  } = context.useWorkspace();
  const { lastUpdated: isWorkspaceReady } = context.useWorkspaceState();

  const {
    open: openPropertyDialog,
    isOpen: isPropertyDialogOpen,
    close: closePropertyDialog,
    setData: setPropertyDialogData,
    data: PropertyDialogData,
  } = context.usePipelinePropertyDialog();
  const { setSelectedCell, fetchPipeline } = context.usePipelineActions();
  const { selectedCell } = context.usePipelineState();

  const {
    data: streams,
    lastUpdated: streamLastUpdated,
  } = context.useStreamState();
  const {
    lastUpdated: connectorLastUpdated,
    data: connectors,
  } = context.useConnectorState();
  const { lastUpdated: topicLastUpdated } = context.useTopicState();

  const { setIsOpen: setIsNewWorkspaceDialogOpen } = useNewWorkspace();

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

  const { create: createTopic, remove: removeTopic } = pipelineUtils.topic();

  const { updateCells, checkCells } = pipelineUtils.pipeline();

  const currentStreamRef = React.useRef(null);
  const isInitialized = React.useRef(false);
  const forceDeleteList = React.useRef({});
  const [isOpen, setIsOpen] = React.useState(false);
  const [currentCellData, setCurrentCellData] = React.useState(null);
  const [url, setUrl] = React.useState(null);

  const { workspaceName, pipelineName } = useParams();
  const { setWorkspaceName, setPipelineName } = context.useApp();

  const handleDialogClose = () => {
    setIsOpen(false);
    forceDeleteList.current = {};
  };

  useEffect(() => {
    currentStreamRef.current = streams;
  }, [streams]);

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
          removeTopic({ ...cells[key] }, paperApi);
          break;

        default:
          break;
      }
    });
  };

  const paperApiRef = useRef(null);
  const cellsMetricsRef = useRef([]);
  const isPaperApiReady = _.has(paperApiRef, 'current.state.isReady');

  useRedirect();

  useEffect(() => {
    if (!isWorkspaceReady) return;
    if (workspaces.length > 0) {
      return setIsNewWorkspaceDialogOpen(false);
    }

    setIsNewWorkspaceDialogOpen(true);
  }, [isWorkspaceReady, setIsNewWorkspaceDialogOpen, workspaces.length]);

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

  const handleSubmit = (params, values, paperApi) => {
    const { cell, topics = [] } = params;
    const { kind } = cell;
    switch (kind) {
      case KIND.source:
      case KIND.sink:
        updateConnector(cell, topics, values, paperApi);
        break;
      case KIND.stream:
        updateStream(cell, topics, values, paperApi);
        break;
      default:
        break;
    }
  };

  // If paper API is not ready, let's reset the pipeline state and re-render again
  useEffect(() => {
    if (!isPaperApiReady && pipelineName) {
      pipelineDispatch({ type: 'resetPipeline' });
    }
  });

  useEffect(() => {
    let timer;
    if (pipelineState.isMetricsOn) {
      if (currentPipeline) {
        timer = setInterval(async () => {
          const res = await fetchPipeline(currentPipeline.name);
          if (isPaperApiReady) {
            const metrics = res.data[0].objects.filter(
              object => object.kind !== KIND.topic,
            );
            paperApiRef.current.updateMetrics(metrics);
            cellsMetricsRef.current = metrics;
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
      setWorkspaceName(workspaceName);
      setPipelineName(pipelineName);
      paperApiRef.current = null;
      isInitialized.current = false;
      setUrl(`${workspaceName}/${pipelineName}`);
    }
  }, [workspaceName, pipelineName, url, setWorkspaceName, setPipelineName]);

  useEffect(() => {
    // Only run this once, there's no need to run this logic twice as
    // that's intentional
    if (!isPaperApiReady) return;
    if (isInitialized.current) return;
    if (!connectorLastUpdated) return;
    if (!topicLastUpdated) return;
    if (!streamLastUpdated) return;
    if (pipelineName !== _.get(currentPipeline, 'name', null)) return;

    checkCells(paperApiRef.current);

    isInitialized.current = true;
  }, [
    checkCells,
    connectorLastUpdated,
    currentPipeline,
    isPaperApiReady,
    pipelineName,
    streamLastUpdated,
    topicLastUpdated,
  ]);

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

    await removeTopic(topic, paperApi);
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
      {currentWorkspace && (
        <>
          {currentPipeline && (
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
                        const activePanel =
                          pipelineState.toolboxExpanded[panel];

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
                            createTopic(cellData, paperApi);
                            break;

                          default:
                            break;
                        }
                      }}
                      onCellConfig={(cellData, paperApi) => {
                        const { displayName, className, kind, name } = cellData;
                        let targetCell;
                        switch (kind) {
                          case KIND.source:
                          case KIND.sink:
                            const { classInfos } = currentWorker;
                            targetCell = classInfos.filter(
                              classInfo => classInfo.className === className,
                            );
                            break;
                          case KIND.stream:
                            targetCell = currentStreamRef.current.find(
                              stream => stream.name === name,
                            ).classInfos;
                            break;
                          default:
                            break;
                        }
                        openPropertyDialog();
                        setPropertyDialogData({
                          title: `Edit the property of ${displayName} ${kind} connector`,
                          classInfo: targetCell[0],
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
                        let topicRes;
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
                            topicRes = await createTopic(
                              ({
                                id: topic.id,
                                name: topic.name,
                                className: topic.className,
                              } = topic),
                              paperApi,
                            );
                            if (topicRes.error) {
                              return;
                            }
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
                            topicRes = await createTopic(
                              ({
                                id: topic.id,
                                name: topic.name,
                                className: topic.className,
                              } = topic),
                              paperApi,
                            );
                            if (topicRes.error) {
                              return;
                            }
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
                            topicRes = await createTopic(
                              ({
                                id: topic.id,
                                name: topic.name,
                                className: topic.className,
                              } = topic),
                              paperApi,
                            );
                            if (topicRes.error) {
                              return;
                            }
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
                            topicRes = await createTopic(
                              ({
                                id: topic.id,
                                name: topic.name,
                                className: topic.className,
                              } = topic),
                              paperApi,
                            );
                            if (topicRes.error) {
                              return;
                            }
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
                            stream.to.find(
                              topic => topic.name === cellData.name,
                            ),
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
                </PaperContext.Provider>
              </PipelineStateContext.Provider>
            </PipelineDispatchContext.Provider>
          )}
        </>
      )}

      <IntroDialog quickModeText={'QUICK CREATE'} />
      <NodeDialog />
      <PipelinePropertyDialog
        isOpen={isPropertyDialogOpen}
        onClose={closePropertyDialog}
        data={PropertyDialogData}
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
  );
});

export default Pipeline;
