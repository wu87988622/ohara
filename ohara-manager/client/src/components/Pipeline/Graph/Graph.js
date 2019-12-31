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
import { useTheme } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import * as joint from 'jointjs';
import { get } from 'lodash';

import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import { useSnackbar } from 'context/SnackbarContext';
import { Paper, PaperWrapper } from './GraphStyles';
import { usePrevious, useMountEffect } from 'utils/hooks';
import { updateCurrentCell, createConnection } from './graphUtils';
import { useZoom, useCenter } from './GraphHooks';
import {
  usePipelineActions,
  usePipelineState,
  useGraphSettingDialog,
  useWorkspace,
} from 'context';
import ConnectorGraph from './Connector/ConnectorGraph';
import TopicGraph from '../Graph/Topic/TopicGraph';

const Graph = props => {
  const { palette } = useTheme();
  const [initToolboxList, setInitToolboxList] = useState(0);
  const {
    setZoom,
    paperScale,
    setPaperScale,
    isFitToContent,
    setIsFitToContent,
  } = useZoom();

  const { setCenter, isCentered, setIsCentered } = useCenter();
  const { setSelectedCell, updatePipeline } = usePipelineActions();
  const { currentWorker, currentPipeline } = useWorkspace();
  const { selectedCell } = usePipelineState();
  const { open: openSettingDialog, setData } = useGraphSettingDialog();
  const showMessage = useSnackbar();

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

  let graph = useRef(null);
  let paper = useRef(null);
  let dragStartPosition = useRef(null);
  let currentCell = useRef(null);

  useMountEffect(() => {
    const renderGraph = () => {
      const namespace = joint.shapes;
      graph.current = new joint.dia.Graph({}, { cellNamespace: namespace });
      paper.current = new joint.dia.Paper(
        {
          el: document.getElementById('paper'),
          model: graph.current,
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

      paper.current.on('cell:pointerclick', cellView => {
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
        const links = graph.current.getLinks();

        if (links.length > 0) {
          const currentLink = links.find(link => !link.get('target').id);

          if (currentLink) {
            createConnection({
              currentLink,
              cellView,
              graph,
              paper,
              showMessage,
              resetLink,
              setInitToolboxList,
            });
          }
        }
      });

      paper.current.on('link:pointerclick', linkView => {
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
      paper.current.on('cell:mouseenter', cellViewOrLinkView => {
        cellViewOrLinkView.highlight();
      });

      paper.current.on('cell:mouseleave', cellViewOrLinkView => {
        if (cellViewOrLinkView.model.isLink()) {
          cellViewOrLinkView.unhighlight();
        } else {
          // Keep cell menu when necessary
          if (cellViewOrLinkView.model.attributes.menuDisplay === 'none') {
            cellViewOrLinkView.unhighlight();
          }
        }
      });

      paper.current.on('blank:pointerdown', (event, x, y) => {
        // Using the scales from paper itself instead of our
        // paperScale state since it will cause re-render
        // which destroy all graphs on current paper...
        dragStartPosition.current = {
          x: x * paper.current.scale().sx,
          y: y * paper.current.scale().sy,
        };

        paper.current.$el.addClass('is-being-grabbed');
      });

      paper.current.on('blank:pointerclick', () => {
        resetAll();
        resetLink();
        currentCell.current = null;
        setSelectedCell(null);
      });

      paper.current.on('cell:pointerup blank:pointerup', event => {
        if (dragStartPosition.current) {
          delete dragStartPosition.current.x;
          delete dragStartPosition.current.y;
        }

        if (
          get(event, 'options.model.attributes.type', null) === 'html.Element'
        ) {
          updatePipeline({
            name: currentPipeline.name,
            group: currentPipeline.group,
            tags: graph.current.toJSON(),
          });
        }

        updateCurrentCell(currentCell);
        setIsCentered(false);
        paper.current.$el.removeClass('is-being-grabbed');
      });

      graph.current.on('change:target', link => {
        if (link.get('target').id) {
          updatePipeline({
            name: currentPipeline.name,
            group: currentPipeline.group,
            tags: graph.current.toJSON(),
          });
        }
      });

      graph.current.on('add', cell => {
        if (cell.attributes.isTemporary) return;
        if (cell.attributes.isFetch) return;
        if (cell.attributes.type === 'standard.Link') {
          if (!cell.get('attributes.target.id', null)) return;
        }
        updatePipeline({
          name: currentPipeline.name,
          group: currentPipeline.group,
          tags: {
            cells: graph.current
              .toJSON()
              .cells.filter(cell => !cell.isTemporary),
          },
        });
      });
    };

    const resetAll = () => {
      paper.current.findViewsInArea(paper.current.getArea()).forEach(cell => {
        cell.model.attributes.menuDisplay = 'none';
        cell.unhighlight();
      });

      const views = paper.current._views;
      Object.keys(views).forEach(key => {
        if (!views[key].$box) return;
        views[key].updateBox();
      });
    };

    const resetLink = () => {
      // Remove link tools that were added in the previous event
      paper.current.removeTools();

      const links = graph.current.getLinks();
      if (links.length > 0) {
        const currentLink = links.find(link => !link.get('target').id);
        if (currentLink) currentLink.remove();
      }
    };

    renderGraph();
  });

  useEffect(() => {
    if (!currentPipeline) return;
    const cells = currentPipeline.tags.cells ? currentPipeline.tags.cells : [];

    cells
      .filter(cell => cell.type === 'html.Element')
      .forEach(cell => {
        if (
          graph.current != null &&
          paper.current !== null &&
          currentWorker !== null
        ) {
          switch (cell.classType) {
            case 'source':
            case 'sink':
            case 'stream':
              const className = cell.params.cellInfo.className;
              const classInfo = currentWorker.classInfos.filter(
                classInfo => classInfo.className === className,
              )[0];

              graph.current.addCell(
                ConnectorGraph({
                  ...cell.params,
                  id: cell.id,
                  paper,
                  graph,
                  openSettingDialog,
                  setData,
                  classInfo,
                  isFetch: true,
                  cellInfo: {
                    ...cell.params.cellInfo,
                    position: cell.position,
                  },
                  setInitToolboxList,
                }),
              );
              break;

            case 'topic':
              graph.current.addCell(
                TopicGraph({
                  ...cell.params,
                  id: cell.id,
                  graph,
                  paper,
                  isFetch: true,
                  cellInfo: {
                    ...cell.params.cellInfo,
                    position: cell.position,
                  },
                }),
              );
              break;

            default:
              break;
          }
        }
      });
    if (graph.current.getElements().length > 0) {
      cells
        .filter(cell => cell.type === 'standard.Link')
        .forEach(cell => {
          const link = new joint.shapes.standard.Link();
          link.source({ id: cell.source.id });
          link.target({ id: cell.target.id });
          link.attr({
            line: { stroke: '#9e9e9e' },
          });
          link.addTo(graph.current);
        });
    }

    setInitToolboxList(prevState => prevState + 1);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPipeline.tags.cells, currentWorker]);

  const prevPaperScale = usePrevious(paperScale);
  useEffect(() => {
    // Prevent rescale again
    if (prevPaperScale === paperScale) return;
    if (isFitToContent) return;

    paper.current.scale(paperScale);

    updateCurrentCell(currentCell);
    setIsCentered(false);
  }, [isFitToContent, paperScale, prevPaperScale, setIsCentered]);

  useEffect(() => {
    if (!isFitToContent) return;

    paper.current.scaleContentToFit({
      padding: 30,
      maxScale: 1,
    });

    // This update is needed so the scale which displays on zoom in/out
    // dropdown will be reflected
    setPaperScale(paper.current.scale().sx);
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
        paper.current.translate(
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
        handleCenter={() => {
          // We don't want to re-center again
          if (!isCentered) {
            setCenter({ paper, currentCell, paperScale });

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
          paper={paper}
          graph={graph}
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
