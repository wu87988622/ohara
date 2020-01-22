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
import * as paperUtils from './PaperUtils';
import { KIND } from 'const';

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
    start: startConnector,
    stop: stopConnector,
    remove: removeConnector,
  } = paperUtils.connector();

  const { create: createTopic } = paperUtils.topic();

  const { create: createStream } = paperUtils.stream();

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
                    switch (cellData.kind) {
                      case KIND.sink:
                      case KIND.source:
                        if (!cellData.isTemporary) {
                          createConnector(cellData, paperApi);
                        }
                        break;

                      case KIND.stream:
                        if (!cellData.isTemporary) {
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
                  onCellStart={(cellData, paperApi) =>
                    startConnector(cellData, paperApi)
                  }
                  onCellStop={(cellData, paperApi) =>
                    stopConnector(cellData, paperApi)
                  }
                  onCellRemove={(cellData, paperApi) =>
                    removeConnector(cellData, paperApi)
                  }
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
