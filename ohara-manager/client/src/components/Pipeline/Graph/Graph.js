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

import Toolbar from '../Toolbar';
import Toolbox from '../Toolbox';
import { useSnackbar } from 'context/SnackbarContext';
import { Paper, PaperWrapper } from './GraphStyles';
import { usePrevious } from 'utils/hooks';
import { updateCurrentCell } from './graphUtils';
import { useZoom, useCenter } from './GraphHooks';

const Graph = props => {
  const { palette } = useTheme();
  const [hasSelectedCell, setHasSelectedCell] = useState(false);
  const {
    setZoom,
    paperScale,
    setPaperScale,
    isFitToContent,
    setIsFitToContent,
  } = useZoom();

  const { setCenter, isCentered, setIsCentered } = useCenter();
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

  useEffect(() => {
    const renderGraph = () => {
      graph.current = new joint.dia.Graph();
      paper.current = new joint.dia.Paper({
        el: document.getElementById('paper'),
        model: graph.current,
        width: '100%',
        height: '100%',

        // Grid settings
        gridSize: 10,
        drawGrid: { name: 'dot', args: { color: palette.grey[300] } },

        // Default origin is in the paper center, reset this so it's
        // Easier to reason about
        origin: { x: 0, y: 0 },

        defaultConnectionPoint: { name: 'boundary' },
        defaultAnchor: {
          name: 'modelCenter',
        },
        background: {
          color: palette.common.white,
        },

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
      });

      paper.current.on('cell:pointerclick', cellView => {
        currentCell.current = {
          cellView,
          bBox: {
            ...cellView.getBBox(),
            ...cellView.getBBox().center(),
          },
        };

        setHasSelectedCell(true);

        if (!cellView.$box) return;

        resetAll(paper.current);
        cellView.highlight();
        cellView.model.attributes.menuDisplay = 'block';
        cellView.updateBox();
        const links = graph.current.getLinks();

        if (links.length > 0) {
          const disConnectLink = links.filter(
            link => !link.attributes.target.id,
          );

          if (disConnectLink.length > 0) {
            // Connect to the target cell
            const targetId = cellView.model.get('id');
            const targetType = cellView.model.get('classType');
            const targetTitle = cellView.model.get('title');
            const targetConnectedLinks = graph.current.getConnectedLinks(
              cellView.model,
            );

            const sourceId = disConnectLink[0].get('source').id;
            const sourceType = graph.current.getCell(sourceId).attributes
              .classType;
            const sourceCell = graph.current.getCell(sourceId);
            const sourceConnectedLinks = graph.current.getConnectedLinks(
              sourceCell,
            );
            const sourceTitle = sourceCell.get('title');

            const isLoopLink = () => {
              return targetConnectedLinks.some(link => {
                return (
                  sourceId === link.get('source').id ||
                  sourceId === link.get('target').id
                );
              });
            };

            const handleError = (message = false) => {
              if (message) showMessage(message);

              resetLink();
            };

            // Cell connection logic
            if (targetId === sourceId) {
              // A cell cannot connect to itself, not throwing a
              // message out here since the behavior is not obvious
              handleError();
            } else if (targetType === 'source') {
              handleError(`Target ${targetTitle} is a source!`);
            } else if (
              sourceType === targetType &&
              (sourceType !== 'stream' && targetType !== 'stream')
            ) {
              handleError(
                `Cannot connect a ${sourceType} to another ${targetType}, they both have the same type`,
              );
            } else if (isLoopLink()) {
              handleError(
                `A connection is already in place for these two cells`,
              );
            } else {
              const hasMoreThanOneTarget = sourceConnectedLinks.length >= 2;
              const hasSource = targetConnectedLinks.length !== 0;

              if (sourceType === 'source' && targetType === 'sink') {
                if (hasMoreThanOneTarget) {
                  return handleError(
                    `The source ${sourceTitle} is already connected to a target`,
                  );
                }

                if (hasSource) {
                  return handleError(
                    `The target ${targetTitle} is already connected to a source `,
                  );
                }
              }

              if (sourceType === 'source' && targetType === 'stream') {
                const isTargetConnectedBySource = targetConnectedLinks.some(
                  link => link.getSourceCell().get('classType') === 'source',
                );

                if (hasMoreThanOneTarget) {
                  return handleError(
                    `The source ${sourceTitle} is already connected to a target`,
                  );
                }

                if (isTargetConnectedBySource) {
                  return handleError(
                    `The target ${targetTitle} already has a connection!`,
                  );
                }
              }

              if (sourceType === 'source' && targetType === 'topic') {
                if (hasMoreThanOneTarget) {
                  return handleError(
                    `The source ${sourceTitle} is already connected to a target`,
                  );
                }
              }

              if (sourceType === 'topic' && targetType === 'sink') {
                if (hasSource) {
                  return handleError(
                    `The target ${targetTitle} is already connected to a source `,
                  );
                }
              }

              if (sourceType === 'stream' && targetType === 'sink') {
                if (hasMoreThanOneTarget) {
                  return handleError(
                    `The source ${sourceTitle} is already connected to a target`,
                  );
                }

                if (hasSource) {
                  return handleError(
                    `The target ${targetTitle} is already connected to a source `,
                  );
                }
              }

              // Link to the target cell
              disConnectLink[0].target({ id: cellView.model.id });
            }
          }
        }
      });

      paper.current.on('link:pointerclick', linkView => {
        const targetId = linkView.model.get('target').id;
        if (!targetId) return; // Prevents users accidentally click on the link while connecting

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
      paper.current.on('cell:mouseenter', cellView => {
        if (cellView.model.isLink()) {
          // Prevents users accidentally hover on the link while connecting
          const linkView = cellView;
          const targetId = linkView.model.get('target').id;
          if (!targetId) return;
        }

        cellView.highlight();
      });

      paper.current.on('cell:mouseleave', cellView => {
        if (cellView.model.isLink()) {
          cellView.unhighlight();
        } else {
          // Keep cell menu when necessary
          if (cellView.model.attributes.menuDisplay === 'none') {
            cellView.unhighlight();
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
        setHasSelectedCell(false);
      });

      paper.current.on('cell:pointerup blank:pointerup', () => {
        if (dragStartPosition.current) {
          delete dragStartPosition.current.x;
          delete dragStartPosition.current.y;
        }

        updateCurrentCell(currentCell);
        setIsCentered(false);
        paper.current.$el.removeClass('is-being-grabbed');
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
        const disConnectLink = links.filter(link => !link.attributes.target.id);
        if (disConnectLink.length > 0) {
          disConnectLink[0].remove();
        }
      }
    };

    renderGraph();
  }, [
    palette.common.white,
    palette.grey,
    palette.primary,
    setIsCentered,
    showMessage,
  ]);

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
        hasSelectedCell={hasSelectedCell}
      />
      <PaperWrapper>
        <Paper id="paper"></Paper>
        <Toolbox
          isOpen={isToolboxOpen}
          expanded={toolboxExpanded}
          handleClick={handleToolboxClick}
          handleClose={handleToolboxClose}
          paper={paper.current}
          graph={graph.current}
          toolboxKey={toolboxKey}
          setToolboxExpanded={setToolboxExpanded}
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
