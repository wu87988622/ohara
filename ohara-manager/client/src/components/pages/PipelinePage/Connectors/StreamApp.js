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

import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import { Field, Form } from 'react-final-form';
import { get, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as streamApi from 'api/streamApi';
import Controller from './Controller';
import AutoSave from './AutoSave';
import { isEmptyStr } from 'utils/commonUtils';
import { STREAM_APP_STATES, STREAM_APP_ACTIONS } from 'constants/pipelines';
import { Box } from 'components/common/Layout';
import { Label } from 'components/common/Form';
import { InputField, SelectField } from 'components/common/FormFields';
import { findByGraphName } from '../pipelineUtils/commonUtils';
import { graph as graphPropType } from 'propTypes/pipeline';
import * as s from './styles';

class StreamApp extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object,
    }).isRequired,
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    updateGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    pipelineTopics: PropTypes.array.isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
    pipeline: PropTypes.shape({
      workerClusterName: PropTypes.string.isRequired,
    }).isRequired,
  };

  selectMaps = {
    fromTopics: 'currFromTopic',
  };

  state = {
    streamAppName: null,
    streamApp: null,
    state: null,
    topics: [],
  };

  componentDidMount() {
    const { match, pipelineTopics } = this.props;
    const { connectorName: streamAppName } = match.params;

    this.setState({ streamAppName, pipelineTopics }, () => {
      this.fetchStreamApp(streamAppName);
    });
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { pipelineTopics: currTopics } = this.props;
    const { connectorName: prevConnectorName } = prevProps.match.params;
    const { connectorName: currConnectorName } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ topics: currTopics });
    }

    if (prevConnectorName !== currConnectorName) {
      const streamAppName = currConnectorName;

      this.setState({ streamAppName }, () => {
        this.fetchStreamApp(streamAppName);
      });
    }
  }

  fetchStreamApp = async name => {
    const res = await streamApi.fetchProperty(name);
    const streamApp = get(res, 'data.result', null);

    if (!isEmpty(streamApp)) {
      this.setState({ streamApp });
    }
  };

  handleSave = async ({ name, instances, from, to }) => {
    const { graph, updateGraph } = this.props;
    const { streamAppName, streamApp } = this.state;
    const { name: jarName } = streamApp.jar;
    const fromTopic = from ? [from] : [];
    const toTopic = to ? [to] : [];

    const params = {
      name: streamAppName,
      jarName,
      instances,
      from: fromTopic,
      to: toTopic,
    };

    const res = await streamApi.updateProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const [streamApp] = graph.filter(g => g.name === streamAppName);
      const [prevFromTopic] = graph.filter(g => g.to.includes(streamAppName));
      const isToUpdate = streamApp.to[0] !== toTopic[0];

      // To topic update
      if (isToUpdate) {
        const currStreamApp = findByGraphName(graph, streamAppName);
        const toUpdate = { ...currStreamApp, to: toTopic };
        updateGraph({ update: toUpdate });
      } else {
        // From topic update
        let currFromTopic = findByGraphName(graph, fromTopic[0]);
        let fromUpdate;

        if (currFromTopic) {
          fromUpdate = [...new Set([...currFromTopic.to, streamAppName])];
        } else {
          if (prevFromTopic) {
            fromUpdate = prevFromTopic.to.filter(t => t !== streamAppName);
          } else {
            fromUpdate = [];
          }

          currFromTopic = prevFromTopic;
        }

        let update;
        if (!currFromTopic) {
          update = { ...currFromTopic };
        } else {
          update = {
            ...currFromTopic,
            to: fromUpdate,
          };
        }

        updateGraph({
          update,
          isFromTopic: true,
          streamAppName,
          updatedName: params.name,
        });
      }
    }
  };

  handleStartStreamApp = async () => {
    await this.triggerStreamApp(STREAM_APP_ACTIONS.start);
  };

  handleStopStreamApp = async () => {
    await this.triggerStreamApp(STREAM_APP_ACTIONS.stop);
  };

  handleDeleteConnector = async () => {
    const { match, refreshGraph, history } = this.props;
    const { connectorName: streamAppName, pipelineName } = match.params;

    const res = await streamApi.deleteProperty(streamAppName);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const { name: connectorName } = this.state.streamApp;
      toastr.success(`${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${connectorName}`);
      await refreshGraph();

      const path = `/pipelines/edit/${pipelineName}`;
      history.push(path);
    }
  };

  triggerStreamApp = async action => {
    const { streamAppName } = this.state;
    let res;
    if (action === STREAM_APP_ACTIONS.start) {
      res = await streamApi.startStreamApp(streamAppName);
    } else {
      res = await streamApi.stopStreamApp(streamAppName);
    }
    this.handleTriggerStreamAppResponse(action, res);
  };

  handleTriggerStreamAppResponse = (action, res) => {
    const isSuccess = get(res, 'data.isSuccess', false);
    if (!isSuccess) return;

    const { graph, updateGraph } = this.props;
    const { streamAppName } = this.state;
    const state = get(res, 'data.result.state');
    this.setState({ state });

    const currStreamApp = findByGraphName(graph, streamAppName);
    const update = { ...currStreamApp, state };
    updateGraph({ update });

    if (action === STREAM_APP_ACTIONS.start) {
      if (state === STREAM_APP_STATES.running) {
        toastr.success(MESSAGES.STREAM_APP_START_SUCCESS);
      } else {
        toastr.error(MESSAGES.CANNOT_START_STREAM_APP_ERROR);
      }
    } else if (action === STREAM_APP_ACTIONS.stop) {
      toastr.success(MESSAGES.STREAM_APP_STOP_SUCCESS);
    }
  };

  render() {
    const { updateHasChanges, pipelineTopics } = this.props;
    const { streamApp } = this.state;

    if (!streamApp) return null;

    const { name, instances, jar, from, to } = streamApp;
    const { name: jarName } = jar;
    const fromTopic = pipelineTopics.find(({ name }) => name === from[0]);
    const toTopic = pipelineTopics.find(({ name }) => name === to[0]);

    const initialValues = {
      name: isEmptyStr(name) ? 'Untitled stream app' : name,
      instances: String(instances),
      from: !isEmpty(fromTopic) ? fromTopic.name : null,
      to: !isEmpty(toTopic) ? toTopic.name : null,
    };

    return (
      <>
        <Form
          onSubmit={this.handleSave}
          initialValues={initialValues}
          render={() => (
            <Box>
              <AutoSave
                save={this.handleSave}
                updateHasChanges={updateHasChanges}
              />
              <s.TitleWrapper>
                <s.H5Wrapper>Stream app</s.H5Wrapper>
                <Controller
                  kind="stream app"
                  onStart={this.handleStartStreamApp}
                  onStop={this.handleStopStreamApp}
                  onDelete={this.handleDeleteConnector}
                  show={['start', 'stop', 'delete']}
                />
              </s.TitleWrapper>
              <s.FormRow>
                <s.FormCol width="70%">
                  <Label>Name</Label>
                  <Label>{streamApp.name}</Label>
                </s.FormCol>
                <s.FormCol width="30%">
                  <Label htmlFor="input-instances">Instances</Label>
                  <Field
                    id="input-instances"
                    name="instances"
                    component={InputField}
                    type="number"
                    min={1}
                    max={100}
                    width="100%"
                    placeholder="1"
                  />
                </s.FormCol>
              </s.FormRow>
              <s.FormRow>
                <s.FormCol width="50%">
                  <Label htmlFor="">From topic</Label>
                  <Field
                    id="select-from"
                    name="from"
                    component={SelectField}
                    list={pipelineTopics}
                    width="100%"
                    placeholder="select a from topic..."
                    isObject
                    clearable
                  />
                </s.FormCol>
                <s.FormCol width="50%">
                  <Label>To topic</Label>
                  <Field
                    name="to"
                    component={SelectField}
                    list={pipelineTopics}
                    width="100%"
                    placeholder="select a to topic..."
                    isObject
                    clearable
                  />
                </s.FormCol>
              </s.FormRow>
              <s.FormRow>
                <s.FormCol>
                  <Label>Jar name</Label>
                  <s.JarNameText>{jarName}</s.JarNameText>
                </s.FormCol>
              </s.FormRow>
              <s.FormRow>
                <s.FormCol>
                  <s.ViewTopologyBtn text="View topology" disabled />
                </s.FormCol>
              </s.FormRow>
            </Box>
          )}
        />
      </>
    );
  }
}

export default StreamApp;
