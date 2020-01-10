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

import React, { useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';
import * as joint from 'jointjs';
import { useTheme } from '@material-ui/core/styles';
import { get, isEqual } from 'lodash';

import { KIND } from 'const';
import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import ConnectorGraph from './Connector/ConnectorGraph';
import TopicGraph from '../Graph/Topic/TopicGraph';
import { Paper, PaperWrapper } from './GraphStyles';
import { usePrevious, useMountEffect, useLocalStorage } from 'utils/hooks';
import { updateCurrentCell, createConnection } from './graphUtils';
import { useZoom, useCenter } from './GraphHooks';
import * as context from 'context';

const Graph = props => {
  const { palette } = useTheme();
  const [initToolboxList, setInitToolboxList] = useState(0);
  const [isMetricsOn, setIsMetricsOn] = useLocalStorage(
    'isPipelineMetricsOn',
    null,
  );

  const { setSelectedCell, updatePipeline } = context.usePipelineActions();
  const { currentWorker, currentPipeline } = context.useWorkspace();
  const { selectedCell } = context.usePipelineState();
  const { open: openSettingDialog, setData } = context.useGraphSettingDialog();
  const { data: currentConnector } = context.useConnectorState();
  const { data: currentTopic } = context.useTopicState();
  const { stopTopic, deleteTopic, createTopic } = context.useTopicActions();
  const {
    startConnector,
    stopConnector,
    deleteConnector,
    updateConnector,
  } = context.useConnectorActions();
  const showMessage = context.useSnackbar();

  const {
    setZoom,
    paperScale,
    setPaperScale,
    isFitToContent,
    setIsFitToContent,
  } = useZoom();

  const { setCenter, isCentered, setIsCentered } = useCenter();

  const {
    isToolboxOpen,
    toolboxExpanded,
    handleToolboxClick,
    handleToolbarClick,
    handleToolboxOpen,
    handleToolboxClose,
    toolboxKey,
    setToolboxExpanded,
  } = props;

  let graphRef = useRef(null);
  let paperRef = useRef(null);
  let dragStartPosition = useRef(null);
  let currentCell = useRef(null);
  let isAdding = useRef(null);
  let currentPipelineRef = useRef(null);

  useEffect(() => {
    currentPipelineRef.current = currentPipeline;
  }, [currentPipeline]);

  useMountEffect(() => {
    const namespace = joint.shapes;
    graphRef.current = new joint.dia.Graph({}, { cellNamespace: namespace });

    paperRef.current = new joint.dia.Paper(
      {
        el: document.getElementById('paper'),
        model: graphRef.current,
        width: '100%',
        height: '100%',

        // Grid settings
        gridSize: 10,
        drawGrid: { name: 'dot', args: { color: palette.grey[300] } },

        background: { color: palette.common.white },

        // Tweak the default highlighting to match our theme
        highlighting: {
          default: {
            name: 'stroke',
            options: {
              padding: 4,
              rx: 4,
              ry: 4,
              attrs: {
                'stroke-width': 2,
                stroke: palette.primary.main,
              },
            },
          },
        },

        // Ensures the link should always link to a valid target
        linkPinning: false,

        // Fix es6 module issue with JointJS
        cellViewNamespace: joint.shapes,

        // prevent graph from stepping outside of the paper
        restrictTranslate: true,
      },
      { cellNamespace: namespace },
    );
  });

  useEffect(() => {
    const paper = paperRef.current;
    const graph = graphRef.current;

    const resetAll = () => {
      paper.findViewsInArea(paper.getArea()).forEach(cell => {
        cell.model.attributes.menuDisplay = 'none';
        cell.unhighlight();
      });

      const views = paper._views;
      Object.keys(views).forEach(key => {
        if (!views[key].$box) return;
        views[key].updateBox();
      });
    };

    const resetLink = () => {
      // Remove link tools that were added in the previous event
      paper.removeTools();

      const links = graph.getLinks();
      if (links.length > 0) {
        const currentLink = links.find(link => !link.get('target').id);
        if (currentLink) currentLink.remove();
      }
    };

    paper.on('cell:pointerclick', cellView => {
      currentCell.current = {
        cellView,
        bBox: {
          ...cellView.getBBox(),
          ...cellView.getBBox().center(),
        },
      };

      const { title, classType } = cellView.model.attributes;
      setSelectedCell({
        name: title,
        classType,
      });

      if (!cellView.$box) return;

      resetAll();
      cellView.highlight();
      cellView.model.attributes.menuDisplay = 'block';
      cellView.updateBox();
      const links = graph.getLinks();

      if (links.length > 0) {
        const currentLink = links.find(link => !link.get('target').id);
        if (currentLink) {
          resetLink();
          createConnection({
            currentLink,
            cellView,
            graph: graphRef,
            paper: paperRef,
            showMessage,
            setInitToolboxList,
            createTopic,
            stopTopic,
            deleteTopic,
            updatePipeline,
            updateConnector,
            currentTopic,
            currentPipeline,
          });
        }
      }
    });

    paper.on('link:pointerclick', linkView => {
      resetLink();

      linkView.addTools(
        new joint.dia.ToolsView({
          tools: [
            // Allow users to add vertices on link view
            new joint.linkTools.Vertices(),
            new joint.linkTools.Segments(),
            // Add a custom remove tool
            new joint.linkTools.Remove({
              offset: 15,
              distance: '50%',
              markup: [
                {
                  tagName: 'circle',
                  selector: 'button',
                  attributes: {
                    r: 8,
                    fill: 'grey',
                    cursor: 'pointer',
                  },
                },
                {
                  tagName: 'path',
                  selector: 'icon',
                  attributes: {
                    d: 'M -3 -3 3 3 M -3 3 3 -3',
                    fill: 'none',
                    stroke: '#fff',
                    'stroke-width': 2,
                    'pointer-events': 'none',
                  },
                },
              ],
            }),
          ],
        }),
      );
    });

    // Cell and link hover effect
    paper.on('cell:mouseenter', cellViewOrLinkView => {
      cellViewOrLinkView.highlight();
    });

    paper.on('cell:mouseleave', cellViewOrLinkView => {
      if (cellViewOrLinkView.model.isLink()) {
        cellViewOrLinkView.unhighlight();
      } else {
        // Keep cell menu when necessary
        if (cellViewOrLinkView.model.attributes.menuDisplay === 'none') {
          cellViewOrLinkView.unhighlight();
        }
      }
    });

    paper.on('blank:pointerdown', (event, x, y) => {
      // Using the scales from paper itself instead of our
      // paperScale state since it will cause re-render
      // which destroy all graphs on current paper...
      dragStartPosition.current = {
        x: x * paper.scale().sx,
        y: y * paper.scale().sy,
      };

      paper.$el.addClass('is-being-grabbed');
    });

    paper.on('blank:pointerclick', () => {
      resetAll();
      resetLink();
      currentCell.current = null;
      setSelectedCell(null);
    });

    paper.on('cell:pointerup blank:pointerup', event => {
      if (dragStartPosition.current) {
        delete dragStartPosition.current.x;
        delete dragStartPosition.current.y;
      }

      if (
        get(event, 'options.model.attributes.type', null) === 'html.Element'
      ) {
        const originCell = currentPipeline.tags.cells.filter(
          cell => cell.id === event.options.model.attributes.id,
        )[0];

        if (
          event.options.model.attributes.position.x !== originCell.position.x ||
          event.options.model.attributes.position.y !== originCell.position.y
        ) {
          if (!isEqual(currentPipeline.tags, graph.toJSON())) {
            updatePipeline({
              name: currentPipeline.name,
              tags: graph.toJSON(),
            });
          }
        }
      }
      updateCurrentCell(currentCell);
      setIsCentered(false);
      paper.$el.removeClass('is-being-grabbed');
    });

    graph.on('change:target', link => {
      if (link.get('target').id) {
        if (!isEqual(currentPipeline.tags, graph.toJSON())) {
          updatePipeline({
            name: currentPipeline.name,
            tags: graph.toJSON(),
          });
        }
      }
    });

    graph.on('add', graphCell => {
      if (graphCell.attributes.isTemporary) return;
      if (graphCell.attributes.isFetch) return;
      if (graphCell.attributes.type === 'standard.Link') {
        if (!graphCell.get('attributes.target.id', null)) return;
      }
      const cells = get(currentPipeline, 'tags.cells', []);
      if (!cells.find(cell => cell.id === graphCell.id)) {
        if (isAdding.current === graphCell) return;
        updatePipeline({
          name: currentPipeline.name,
          tags: {
            cells: graph.toJSON().cells.filter(cell => !cell.isTemporary),
          },
        });
        isAdding.current = graphCell;
      }
    });

    return () => {
      paper.off('cell:pointerclick');
      paper.off('link:pointerclick');
      paper.off('cell:mouseenter');
      paper.off('cell:mouseleave');
      paper.off('blank:pointerdown');
      paper.off('blank:pointerclick');
      paper.off('cell:pointerup');
      paper.off('blank:pointerup');
    };
  }, [
    createTopic,
    currentPipeline,
    currentTopic,
    deleteTopic,
    setIsCentered,
    setSelectedCell,
    showMessage,
    stopTopic,
    updateConnector,
    updatePipeline,
  ]);

  useEffect(() => {
    if (isMetricsOn === null) {
      // Defaults to false
      setIsMetricsOn(false);
    }
  }, [isMetricsOn, setIsMetricsOn]);

  useEffect(() => {
    if (!currentPipelineRef.current) return;
    get(currentPipelineRef, 'current.tags.cells', [])
      .filter(cell => cell.type === 'html.Element')
      .forEach(cell => {
        if (
          graphRef.current !== null &&
          paperRef.current !== null &&
          currentWorker !== null
        ) {
          switch (cell.classType) {
            case KIND.source:
            case KIND.sink:
            case KIND.stream:
              const className = cell.params.cellInfo.className;
              const classInfo = currentWorker.classInfos.find(
                classInfo => classInfo.className === className,
              );
              const targetCell = currentPipelineRef.current.objects.find(
                object =>
                  object.name === cell.title && object.kind === cell.classType,
              );

              graphRef.current.addCell(
                ConnectorGraph({
                  ...cell.params,
                  id: cell.id,
                  paper: paperRef,
                  graph: graphRef,
                  openSettingDialog,
                  setData,
                  classInfo,
                  isFetch: true,
                  isMetricsOn,
                  metrics: targetCell && targetCell.metrics,
                  cellInfo: {
                    ...cell.params.cellInfo,
                    position: cell.position,
                  },
                  setInitToolboxList,
                  startConnector,
                  stopConnector,
                  deleteConnector,
                  updatePipeline,
                  currentPipeline: currentPipelineRef.current,
                }),
              );
              break;

            case KIND.topic:
              graphRef.current.addCell(
                TopicGraph({
                  ...cell.params,
                  id: cell.id,
                  graph: graphRef,
                  paper: paperRef,
                  isFetch: true,
                  cellInfo: {
                    ...cell.params.cellInfo,
                    position: cell.position,
                  },
                  stopTopic,
                  deleteTopic,
                  updatePipeline,
                  currentPipeline: currentPipelineRef.current,
                }),
              );
              break;

            default:
              break;
          }
        }
      });
    if (graphRef.current.getElements().length > 0) {
      get(currentPipelineRef, 'current.tags.cells', [])
        .filter(cell => cell.type === 'standard.Link')
        .filter(
          cell =>
            !graphRef.current
              .getLinks()
              .find(
                link =>
                  link.attributes.source.id === cell.source.id &&
                  link.attributes.target.id === cell.target.id,
              ),
        )
        .forEach(cell => {
          const link = new joint.shapes.standard.Link();
          link.source({ id: cell.source.id });
          link.target({ id: cell.target.id });
          link.attr({
            line: { stroke: '#9e9e9e' },
          });
          link.addTo(graphRef.current);
        });
    }
  });

  useEffect(() => {
    if (currentConnector) {
      currentConnector
        .filter(connector => connector.topicKeys.length > 0)
        .forEach(linkedConnector => {
          const connector = currentPipeline.tags.cells
            .filter(cell => cell.type === 'html.Element')
            .find(cell => cell.params.name === linkedConnector.name);

          const topic = currentPipeline.tags.cells
            .filter(cell => cell.type === 'html.Element')
            .filter(cell => cell.classType === KIND.topic)
            .find(
              cell => cell.params.name === linkedConnector.topicKeys[0].name,
            );

          const isLinked = currentPipeline.tags.cells
            .filter(cell => cell.type === 'standard.Link')
            .find(
              cell =>
                cell.source.id === connector.id ||
                cell.target.id === connector.id,
            );
          if (!isLinked && topic) {
            const link = new joint.shapes.standard.Link();
            switch (connector.classType) {
              case KIND.source:
                link.source({ id: connector.id });
                link.target({ id: topic.id });
                link.attr({
                  line: { stroke: '#9e9e9e' },
                });
                link.addTo(graphRef.current);

                if (!isEqual(currentPipeline.tags, graphRef.current.toJSON())) {
                  updatePipeline({
                    name: currentPipeline.name,
                    tags: graphRef.current.toJSON(),
                  });
                }
                break;

              case KIND.sink:
                link.source({ id: topic.id });
                link.target({ id: connector.id });
                link.attr({
                  line: { stroke: '#9e9e9e' },
                });
                link.addTo(graphRef.current);

                if (!isEqual(currentPipeline.tags, graphRef.current.toJSON())) {
                  updatePipeline({
                    name: currentPipeline.name,
                    tags: graphRef.current.toJSON(),
                  });
                }
                break;

              default:
                break;
            }
          }
        });
    }
  }, [
    currentConnector,
    currentPipeline.name,
    currentPipeline.tags,
    updateConnector,
    updatePipeline,
  ]);

  const prevPaperScale = usePrevious(paperScale);
  useEffect(() => {
    // Prevent rescale again
    if (prevPaperScale === paperScale) return;
    if (isFitToContent) return;

    paperRef.current.scale(paperScale);

    updateCurrentCell(currentCell);
    setIsCentered(false);
  }, [isFitToContent, paperScale, prevPaperScale, setIsCentered]);

  useEffect(() => {
    if (!isFitToContent) return;

    paperRef.current.scaleContentToFit({
      padding: 30,
      maxScale: 1,
    });

    // This update is needed so the scale which displays on zoom in/out
    // dropdown will be reflected
    setPaperScale(paperRef.current.scale().sx);
    updateCurrentCell(currentCell);
    setIsCentered(false);
  }, [isFitToContent, setIsCentered, setPaperScale]);

  useEffect(() => {
    document.getElementById('paper').addEventListener('mousemove', event => {
      // Reset the state so we can call fit to content multiple times
      if (isFitToContent) setIsFitToContent(false);

      if (
        dragStartPosition.current &&
        dragStartPosition.current.x &&
        dragStartPosition.current.y
      ) {
        paperRef.current.translate(
          event.offsetX - dragStartPosition.current.x,
          event.offsetY - dragStartPosition.current.y,
        );
      }
    });
  }, [isFitToContent, setIsFitToContent]);

  return (
    <>
      <Toolbar
        isToolboxOpen={isToolboxOpen}
        handleToolboxOpen={handleToolboxOpen}
        handleToolbarClick={handleToolbarClick}
        paperScale={paperScale}
        handleZoom={setZoom}
        handleFit={() => setIsFitToContent(true)}
        isMetricsOn={isMetricsOn}
        setIsMetricsOn={setIsMetricsOn}
        handleCenter={() => {
          // We don't want to re-center again
          if (!isCentered) {
            setCenter({ paper: paperRef, currentCell, paperScale });

            setIsFitToContent(false);
            setIsCentered(true);
          }
        }}
        hasSelectedCell={Boolean(selectedCell)}
      />
      <PaperWrapper>
        <Paper id="paper"></Paper>
        <Toolbox
          isOpen={isToolboxOpen}
          expanded={toolboxExpanded}
          handleClick={handleToolboxClick}
          handleClose={handleToolboxClose}
          paper={paperRef}
          graph={graphRef}
          toolboxKey={toolboxKey}
          setToolboxExpanded={setToolboxExpanded}
          initToolboxList={initToolboxList}
        />
      </PaperWrapper>
    </>
  );
};

Graph.propTypes = {
  isToolboxOpen: PropTypes.bool.isRequired,
  toolboxExpanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    stream: PropTypes.bool.isRequired,
  }).isRequired,
  handleToolboxClick: PropTypes.func.isRequired,
  handleToolboxOpen: PropTypes.func.isRequired,
  handleToolbarClick: PropTypes.func.isRequired,
  handleToolboxClose: PropTypes.func.isRequired,
  toolboxKey: PropTypes.number.isRequired,
  setToolboxExpanded: PropTypes.func.isRequired,
};

export default Graph;
