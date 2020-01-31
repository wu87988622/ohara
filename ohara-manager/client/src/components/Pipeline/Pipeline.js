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
import { usePrevious, useLocalStorage } from 'utils/hooks';
import { PaperWrapper } from './PipelineStyles';
import {
  usePipelineState as usePipelineReducerState,
  useRedirect,
} from './PipelineHooks';
import * as pipelineUtils from './PipelineApiHelper';
import { KIND, CONNECTION_TYPE } from 'const';

export const PaperContext = createContext(null);

const Pipeline = () => {
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
  const { setSelectedCell } = context.usePipelineActions();

  const { setIsOpen: setIsNewWorkspaceDialogOpen } = useNewWorkspace();

  const [
    { isToolboxOpen, toolboxExpanded, toolboxKey },
    pipelineDispatch,
  ] = usePipelineReducerState();

  const [isMetricsOn, setIsMetricsOn] = useLocalStorage(
    'isPipelineMetricsOn',
    null,
  );

  const {
    create: createConnector,
    update: updateConnector,
    start: startConnector,
    stop: stopConnector,
    remove: removeConnector,
    removeLink: removeLinkConnector,
  } = pipelineUtils.connector();

  const {
    create: createStream,
    updateLinkTo: updateStreamLinkTo,
    updateLinkFrom: updateStreamLinkFrom,
    start: startStream,
    stop: stopStream,
    remove: removeStream,
    removeLinkTo: removeStreamLinkTo,
    removeLinkFrom: removeStreamLinkFrom,
  } = pipelineUtils.stream();

  const { create: createTopic, remove: removeTopic } = pipelineUtils.topic();

  const clean = (cells, paperApi) => {
    Object.keys(cells).forEach(key => {
      switch (key) {
        case 'source':
        case 'sink':
          removeLinkConnector({ name: cells[key].name });
          break;

        case 'to':
          removeStreamLinkTo({
            name: cells[key].name,
          });
          break;
        case 'from':
          removeStreamLinkFrom({
            name: cells[key].name,
          });
          break;
        case 'topic':
          removeTopic({ ...cells[key] }, paperApi);
          break;

        default:
          break;
      }
    });
  };

  const paperRef = useRef();
  const isPaperApiReady = _.has(paperRef, 'current.state.isReady');

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

  return (
    <>
      {currentWorkspace && (
        <>
          {currentPipeline && (
            <PaperContext.Provider value={{ ...paperRef.current }}>
              {isPaperApiReady && (
                <Toolbar
                  isToolboxOpen={isToolboxOpen}
                  handleToolboxOpen={() =>
                    pipelineDispatch({ type: 'openToolbox' })
                  }
                  handleToolbarClick={panel => {
                    // Open a particular panel
                    pipelineDispatch({ type: 'resetToolbox' });
                    pipelineDispatch({ type: 'setToolbox', payload: panel });
                  }}
                  isMetricsOn={isMetricsOn}
                  setIsMetricsOn={setIsMetricsOn}
                />
              )}

              <PaperWrapper>
                <Paper
                  ref={paperRef}
                  onCellSelect={element => setSelectedCell(element)}
                  onCellDeselect={() => setSelectedCell(null)}
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
                  onCellConfig={cellData => {
                    const { displayName, className, kind } = cellData;
                    const { classInfos } = currentWorker;
                    const [targetConnector] = classInfos.filter(
                      classInfo => classInfo.className === className,
                    );

                    openPropertyDialog();
                    setPropertyDialogData({
                      title: `Edit the property of ${displayName} ${kind} connector`,
                      classInfo: targetConnector,
                      cellData,
                    });
                  }}
                  onConnect={async (cells, paperApi) => {
                    const {
                      type,
                      source,
                      stream,
                      sink,
                      link,
                      topic,
                      firstLink,
                      secondeLink,
                    } = pipelineUtils.utils.getConnectionOrder(cells);
                    let topicRes;
                    let sourceRes;
                    let sinkRes;
                    let streamRes;
                    switch (type) {
                      case CONNECTION_TYPE.source_topic:
                        updateConnector(
                          { connector: source, topic, link },
                          paperApi,
                        );
                        break;
                      case CONNECTION_TYPE.topic_sink:
                        updateConnector(
                          { connector: sink, topic, link },
                          paperApi,
                        );
                        break;
                      case CONNECTION_TYPE.stream_topic:
                        updateStreamLinkTo({ stream, topic, link }, paperApi);
                        break;
                      case CONNECTION_TYPE.topic_stream:
                        updateStreamLinkFrom({ stream, topic, link }, paperApi);
                        break;
                      case CONNECTION_TYPE.source_topic_sink:
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
                        sourceRes = await updateConnector(
                          { connector: source, topic, link: firstLink },
                          paperApi,
                        );
                        if (sourceRes.error) {
                          clean({ topic });
                          return;
                        }
                        sinkRes = await updateConnector(
                          { connector: sink, topic, link: secondeLink },
                          paperApi,
                        );
                        if (sinkRes.error) {
                          clean({ source, topic });
                          return;
                        }
                        break;
                      case CONNECTION_TYPE.source_topic_stream:
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
                        sourceRes = await updateConnector(
                          { connector: source, topic, link: firstLink },
                          paperApi,
                        );
                        if (sourceRes.error) {
                          clean({ topic });
                          return;
                        }
                        streamRes = await updateStreamLinkFrom(
                          { stream, topic, link },
                          paperApi,
                        );
                        if (streamRes.error) {
                          clean({ source, topic });
                          return;
                        }
                        break;
                      case CONNECTION_TYPE.stream_topic_sink:
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
                        streamRes = await updateStreamLinkTo(
                          { stream, topic, link },
                          paperApi,
                        );
                        if (streamRes.error) {
                          clean({ topic });
                          return;
                        }
                        sinkRes = await updateConnector(
                          { connector: sink, topic, link: secondeLink },
                          paperApi,
                        );
                        if (sinkRes.error) {
                          clean({ to: stream, topic });
                          return;
                        }
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
                  onCellRemove={(cellData, paperApi) => {
                    switch (cellData.kind) {
                      case KIND.sink:
                      case KIND.source:
                        removeConnector(cellData, paperApi);
                        break;

                      case KIND.stream:
                        removeStream(cellData, paperApi);
                        break;

                      case KIND.topic:
                        removeTopic(cellData, paperApi);

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
          )}
        </>
      )}

      <IntroDialog
        quickModeText={workspaces.length > 0 ? 'QUICK CREATE' : 'QUICK START'}
      />
      <NodeDialog />
      <PipelinePropertyDialog
        isOpen={isPropertyDialogOpen}
        handleClose={closePropertyDialog}
        data={PropertyDialogData}
      />
    </>
  );
};

export default Pipeline;
