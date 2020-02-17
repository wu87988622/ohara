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
import Paper from './Paper';
import Toolbar from './Toolbar';
import Toolbox from './Toolbox';
import NodeDialog from 'components/Node/NodeDialog';
import IntroDialog from './IntroDialog';
import PipelinePropertyDialog from './PipelinePropertyDialog';
import { useNewWorkspace } from 'context/NewWorkspaceContext';
import { usePrevious } from 'utils/hooks';
import { PaperWrapper } from './PipelineStyles';
import {
  usePipelineState as usePipelineReducerState,
  useRedirect,
} from './PipelineHooks';
import * as pipelineUtils from './PipelineApiHelper';
import { CONNECTION_TYPE } from './PipelineApiHelper';
import { KIND } from 'const';
import { Dialog } from 'components/common/Dialog';

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
  const [selectTopic, setSelectTopic] = React.useState(false);

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
    const { cell, topic = {} } = params;
    const { kind } = cell;
    switch (kind) {
      case KIND.source:
      case KIND.sink:
        updateConnector(cell, topic, values, paperApi);
        break;
      case KIND.stream:
        updateStream(cell, topic, values, paperApi);
        break;
      default:
        break;
    }
  };

  const { isToolboxOpen, toolboxExpanded, toolboxKey } = pipelineState;

  useEffect(() => {
    let timer;
    if (pipelineState.isMetricsOn) {
      if (currentPipeline) {
        timer = setInterval(async () => {
          const res = await fetchPipeline(currentPipeline.name);
          if (isPaperApiReady) {
            paperApiRef.current.updateMetrics(
              res.data[0].objects.filter(object => object.kind !== KIND.topic),
            );
          }
        }, 5000);
      }
    }

    return () => {
      clearInterval(timer);
    };
  });

  useEffect(() => {
    // Only run this once, there's no need to run this logic twice as
    // that's intentional
    if (!isPaperApiReady) return;
    if (isInitialized.current) return;
    if (!connectorLastUpdated) return;
    if (!topicLastUpdated) return;
    if (!streamLastUpdated) return;

    checkCells(paperApiRef.current);

    isInitialized.current = true;
  }, [
    checkCells,
    connectorLastUpdated,
    isPaperApiReady,
    streamLastUpdated,
    topicLastUpdated,
  ]);

  const forceDelete = async () => {
    const {
      connectors,
      toStreams,
      fromStreams,
      topic,
    } = forceDeleteList.current;
    const paperApi = paperApiRef.current;
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
    setIsOpen(false);
    forceDeleteList.current = {};
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
                      isToolboxOpen={isToolboxOpen}
                      handleToolboxOpen={() =>
                        pipelineDispatch({ type: 'openToolbox' })
                      }
                      handleToolbarClick={panel => {
                        // Open a particular panel
                        pipelineDispatch({ type: 'resetToolbox' });
                        pipelineDispatch({
                          type: 'setToolbox',
                          payload: panel,
                        });
                      }}
                    />
                  )}

                  <PaperWrapper>
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
                      onCellRemove={async (cellData, paperApi) => {
                        switch (cellData.kind) {
                          case KIND.sink:
                          case KIND.source:
                            removeConnector(cellData, paperApi);
                            break;

                          case KIND.stream:
                            removeStream(cellData, paperApi);
                            break;

                          case KIND.topic:
                            const hasTopicConnectors = connectors.filter(
                              connector =>
                                connector.topicKeys.find(
                                  topic => topic.name === cellData.name,
                                ),
                            );
                            const hasTopicToStream = streams.filter(stream =>
                              stream.to.find(
                                topic => topic.name === cellData.name,
                              ),
                            );
                            const hasTopicFromStream = streams.filter(stream =>
                              stream.from.find(
                                topic => topic.name === cellData.name,
                              ),
                            );
                            if (
                              [
                                ...hasTopicConnectors,
                                ...hasTopicToStream,
                                ...hasTopicFromStream,
                              ].length > 0
                            ) {
                              forceDeleteList.current = {
                                connectors: hasTopicConnectors,
                                toStreams: hasTopicToStream,
                                fromStreams: hasTopicFromStream,
                                topic: cellData,
                              };
                              setSelectTopic(cellData.displayName);
                              setIsOpen(true);
                            } else {
                              await removeTopic(cellData, paperApi);
                            }

                            if (cellData.isShared) {
                              // If a shared topic is removed from the Paper, we should
                              // re-render the Toolbox, so the topic can be re-added
                              // into the Paper again
                              pipelineDispatch({ type: 'setToolboxKey' });
                            }
                            break;
                          default:
                            break;
                        }
                      }}
                    />
                    {isPaperApiReady && (
                      <Toolbox
                        isOpen={isToolboxOpen}
                        expanded={toolboxExpanded}
                        pipelineDispatch={pipelineDispatch}
                        toolboxKey={toolboxKey}
                      />
                    )}
                  </PaperWrapper>
                </PaperContext.Provider>
              </PipelineStateContext.Provider>
            </PipelineDispatchContext.Provider>
          )}
        </>
      )}

      <IntroDialog
        quickModeText={workspaces.length > 0 ? 'QUICK CREATE' : 'QUICK START'}
      />
      <NodeDialog />
      <PipelinePropertyDialog
        isOpen={isPropertyDialogOpen}
        onClose={closePropertyDialog}
        data={PropertyDialogData}
        onSubmit={handleSubmit}
      />
      <Dialog
        open={isOpen}
        title={'Force Delete Topic?'}
        children={`Are you sure you want to delete the node: ${selectTopic} ? This action cannot be undone!`}
        confirmText={'FORCE DELETE'}
        handleClose={handleDialogClose}
        handleConfirm={forceDelete}
      />
    </>
  );
});

export default Pipeline;
