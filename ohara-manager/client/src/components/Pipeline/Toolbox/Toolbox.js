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

import React, { useState, useEffect, useRef } from 'react';
import { renderToString } from 'react-dom/server';
import PropTypes from 'prop-types';
import Draggable from 'react-draggable';
import Typography from '@material-ui/core/Typography';
import InputBase from '@material-ui/core/InputBase';
import IconButton from '@material-ui/core/IconButton';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
import SearchIcon from '@material-ui/icons/Search';
import AddIcon from '@material-ui/icons/Add';
import CloseIcon from '@material-ui/icons/Close';
import FlightTakeoffIcon from '@material-ui/icons/FlightTakeoff';
import FlightLandIcon from '@material-ui/icons/FlightLand';
import StorageIcon from '@material-ui/icons/Storage';
import { useParams } from 'react-router-dom';
import * as joint from 'jointjs';
import * as _ from 'lodash';
import * as $ from 'jquery';

import * as fileApi from 'api/fileApi';
import * as inspectApi from 'api/inspectApi';
import { StyledToolbox } from './Styles';
import { useWorkspace } from 'context/WorkspaceContext';
import { useSnackbar } from 'context/SnackbarContext';
import { useAddTopic } from 'context/AddTopicContext';
import { useTopic } from 'context/TopicContext';
import { Label } from 'components/common/Form';
import { AddTopicDialog } from 'components/Topic';

const Toolbox = props => {
  const {
    isOpen: isToolboxOpen,
    expanded,
    handleClose,
    handleClick,
    paper,
    graph,
  } = props;

  const [streamJars, setStreamJars] = useState([]);
  const [streamJarsStatus, setStreamJarsStatus] = useState('loading');
  const { findByWorkspaceName } = useWorkspace();
  const { workspaceName } = useParams();
  const { topics, doFetch: fetchTopics } = useTopic();
  const { setIsOpen: setIsAddTopicOpen } = useAddTopic();
  const [sources, setSources] = useState([]);
  const [sinks, setSinks] = useState([]);
  const showMessage = useSnackbar();

  const currentWorkspace = findByWorkspaceName(workspaceName);

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchTopics(currentWorkspace.settings.name);
  }, [fetchTopics, currentWorkspace.settings.name, currentWorkspace]);

  useEffect(() => {
    if (!currentWorkspace || streamJarsStatus !== 'loading') return;

    const fetchStreamJars = async workspaceName => {
      const files = await fileApi.getAll({ group: workspaceName });
      const sortedFiles = files.sort((a, b) => a.name.localeCompare(b.name));

      setStreamJars(sortedFiles);
      setStreamJarsStatus('loaded');
    };

    fetchStreamJars(currentWorkspace.settings.name);
  }, [currentWorkspace, streamJarsStatus]);

  useEffect(() => {
    if (!currentWorkspace) return;

    const fetchWorkerInfo = async () => {
      const { name, group } = currentWorkspace.settings;
      const data = await inspectApi.getWorkerInfo({ name, group });
      data.classInfos.forEach(info => {
        const className = info.className.split('.').pop();
        if (info.classType === 'source') {
          setSources(prevState => [...prevState, className]);
          return;
        }

        setSinks(prevState => [...prevState, className]);
      });
    };

    fetchWorkerInfo();
  }, [currentWorkspace]);

  const uploadJar = async file => {
    const response = await fileApi.create({
      file,
      group: workspaceName,
    });

    const isSuccess = !_.isEmpty(response);
    if (isSuccess) {
      showMessage('Successfully uploaded the jar');
    }
    setStreamJarsStatus('loading');
  };

  const handleFileSelect = event => {
    const file = event.target.files[0];

    const isDuplicate = () => streamJars.some(jar => jar.name === file.name);

    if (isDuplicate()) {
      return showMessage('The jar name is already taken!');
    }

    if (event.target.files[0]) {
      uploadJar(file);
    }
  };

  let sourceGraph = useRef(null);
  let sinkGraph = useRef(null);
  let topicGraph = useRef(null);

  useEffect(() => {
    if (!sources) return;

    const sourceIcon = renderToString(<FlightTakeoffIcon />);
    const sinkIcon = renderToString(<FlightLandIcon />);
    const topicIcon = renderToString(<StorageIcon />);

    const sharedProps = {
      width: 'auto',
      height: 'auto',
      interactive: false,
      // this fixes JointJs cannot properly render these html elements in es6 modules: https://github.com/clientIO/joint/issues/1134
      cellViewNamespace: joint.shapes,
    };

    const renderToolbox = () => {
      sourceGraph.current = new joint.dia.Graph();
      const sourcePaper = new joint.dia.Paper({
        el: document.getElementById('source-list'),
        model: sourceGraph.current,
        ...sharedProps,
      });

      sinkGraph.current = new joint.dia.Graph();
      const sinkPaper = new joint.dia.Paper({
        el: document.getElementById('sink-list'),
        model: sinkGraph.current,
        ...sharedProps,
      });

      topicGraph.current = new joint.dia.Graph();
      const topicPaper = new joint.dia.Paper({
        el: document.getElementById('topic-list'),
        model: topicGraph.current,
        ...sharedProps,
      });

      // Create a custom element.
      joint.shapes.html = {};
      joint.shapes.html.Element = joint.shapes.basic.Rect.extend({
        defaults: joint.util.deepSupplement(
          {
            type: 'html.Element',
            attrs: {
              rect: { stroke: 'none', fill: 'transparent' },
            },
          },
          joint.shapes.basic.Rect.prototype.defaults,
        ),
      });

      // Create a custom view for that element that displays an HTML div above it.
      joint.shapes.html.ElementView = joint.dia.ElementView.extend({
        template: [
          `<div class="item">
          <span class="icon"></span>
          <label></label>
          </div>`,
        ],

        initialize() {
          _.bindAll(this, 'updateBox');
          joint.dia.ElementView.prototype.initialize.apply(this, arguments);
          this.$box = $(_.template(this.template)());
          // Update the box position whenever the underlying model changes.
          this.model.on('change', this.updateBox, this);
          this.updateBox();
        },
        render() {
          joint.dia.ElementView.prototype.render.apply(this, arguments);
          this.paper.$el.append(this.$box);
          this.updateBox();
          return this;
        },

        updateBox() {
          // Updating the HTML with a data stored in the cell model.
          this.$box.find('label').text(this.model.get('label'));
          this.$box.find('.icon').html(this.model.get('icon'));
        },
      });

      // Create JointJS elements and add them to the graph as usual.
      sources.forEach((source, index) => {
        sourceGraph.current.addCell(
          new joint.shapes.html.Element({
            position: { x: 10, y: index * 40 },
            size: { width: 272 - 8 * 2, height: 40 },
            label: source,
            icon: sourceIcon,
          }),
        );
      });

      sinks.forEach((sink, index) => {
        sinkGraph.current.addCell(
          new joint.shapes.html.Element({
            position: { x: 10, y: index * 40 },
            size: { width: 272 - 8 * 2, height: 40 },
            label: sink,
            icon: sinkIcon,
          }),
        );
      });

      topics.forEach((topic, index) => {
        topicGraph.current.addCell(
          new joint.shapes.html.Element({
            position: { x: 10, y: index * 40 },
            size: { width: 272 - 8 * 2, height: 40 },
            label: topic.settings.name,
            icon: topicIcon,
          }),
        );
      });

      [sourcePaper, sinkPaper, topicPaper].forEach(toolPaper => {
        // Add "hover" state in items, I cannot figure out how to do
        // this when initializing the HTML elements...
        toolPaper.on('cell:mouseenter', function(cellView) {
          cellView.$box.css('backgroundColor', 'rgba(0, 0, 0, 0.08)');
        });

        toolPaper.on('cell:mouseleave', function(cellView) {
          cellView.$box.css('backgroundColor', 'transparent');
        });

        toolPaper.on('cell:pointerdown', function(cellView, e, x, y) {
          $('#paper').append(
            '<div id="flying-paper" class="flying-paper"></div>',
          );

          const flyingGraph = new joint.dia.Graph();
          new joint.dia.Paper({
            el: $('#flying-paper'),
            width: 160,
            height: 50,
            model: flyingGraph,
            cellViewNamespace: joint.shapes,
            interactive: false,
          });

          const flyingShape = cellView.model.clone();

          const pos = cellView.model.position();
          const offset = {
            x: x - pos.x,
            y: y - pos.y,
          };

          flyingShape.position(0, 0);
          flyingGraph.addCell(flyingShape);

          $('#flying-paper').offset({
            left: e.pageX - offset.x,
            top: e.pageY - offset.y,
          });

          function isInsidePaper() {
            const target = paper.$el.offset();
            const x = e.pageX;
            const y = e.pageY;

            return (
              x > target.left &&
              x < target.left + paper.$el.width() &&
              y > target.top &&
              y < target.top + paper.$el.height()
            );
          }

          $('#paper').on('mousemove.fly', e => {
            $('#flying-paper').offset({
              left: e.pageX - offset.x,
              top: e.pageY - offset.y,
            });
          });

          $('#paper').on('mouseup.fly', e => {
            const x = e.pageX;
            const y = e.pageY;
            const target = paper.$el.offset();

            // Dropped over paper ?
            if (isInsidePaper()) {
              const connector = new joint.shapes.basic.Rect({
                position: {
                  x: x - target.left - offset.x,
                  y: y - target.top - offset.y,
                },
                size: { width: 100, height: 40 },
                attrs: { text: { text: 'Connector' }, rect: { magnet: true } },
              });

              graph.addCell(connector);
            }

            $('#paper')
              .off('mousemove.fly')
              .off('mouseup.fly');
            flyingShape.remove();

            $('#flying-paper').remove();
          });
        });
      });
    };

    renderToolbox();
  }, [graph, paper, sinks, sources, topics]);

  return (
    <Draggable bounds="parent" handle=".box-title">
      <StyledToolbox className={`toolbox ${isToolboxOpen ? 'is-open' : ''}`}>
        <div className="title box-title">
          <Typography variant="subtitle1">Toolbox</Typography>
          <IconButton onClick={handleClose}>
            <CloseIcon />
          </IconButton>
        </div>
        <IconButton>
          <SearchIcon />
        </IconButton>
        <InputBase placeholder="Search topic & connector..." />

        <div className="toolbox-body">
          <ExpansionPanel
            square
            expanded={expanded.source}
            defaultExpanded={true}
          >
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('source')}
            >
              <Typography variant="subtitle1">Source</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="source-list" className="toolbar-list"></div>
              </List>

              <div className="add-button">
                <IconButton>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">
                  Add source connectors
                </Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <ExpansionPanel square expanded={expanded.topic}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('topic')}
            >
              <Typography variant="subtitle1">Topic</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="topic-list" className="toolbar-list"></div>
              </List>

              <div className="add-button">
                <IconButton onClick={() => setIsAddTopicOpen(true)}>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">Add topics</Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <AddTopicDialog />

          <ExpansionPanel square expanded={expanded.streamApp}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('streamApp')}
            >
              <Typography variant="subtitle1">StreamApp</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                {streamJars.map(jar => (
                  <ListItem key={jar.name} button>
                    <ListItemIcon>
                      <StorageIcon />
                    </ListItemIcon>
                    <ListItemText primary={jar.name} />
                  </ListItem>
                ))}
              </List>

              <div className="add-button">
                <input
                  id="fileInput"
                  accept=".jar"
                  type="file"
                  onChange={handleFileSelect}
                  onClick={event => {
                    /* Allow file to be added multiple times */
                    event.target.value = null;
                  }}
                />
                <IconButton>
                  <Label htmlFor="fileInput">
                    <AddIcon />
                  </Label>
                </IconButton>

                <Typography variant="subtitle2">Add stream apps</Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>

          <ExpansionPanel square expanded={expanded.sink}>
            <ExpansionPanelSummary
              className="panel-title"
              expandIcon={<ExpandMoreIcon />}
              onClick={() => handleClick('sink')}
            >
              <Typography variant="subtitle1">Sink</Typography>
            </ExpansionPanelSummary>
            <ExpansionPanelDetails className="detail">
              <List disablePadding>
                <div id="sink-list" className="toolbar-list"></div>
              </List>

              <div className="add-button">
                <IconButton>
                  <AddIcon />
                </IconButton>
                <Typography variant="subtitle2">Add sink connectors</Typography>
              </div>
            </ExpansionPanelDetails>
          </ExpansionPanel>
        </div>
      </StyledToolbox>
    </Draggable>
  );
};

Toolbox.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  handleClose: PropTypes.func.isRequired,
  handleClick: PropTypes.func.isRequired,
  expanded: PropTypes.shape({
    topic: PropTypes.bool.isRequired,
    source: PropTypes.bool.isRequired,
    sink: PropTypes.bool.isRequired,
    streamApp: PropTypes.bool.isRequired,
  }).isRequired,
  paper: PropTypes.any,
  graph: PropTypes.any,
};

export default Toolbox;
