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
import { findByGraphId } from '../pipelineUtils/commonUtils';
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
    streamAppId: null,
    streamApp: null,
    state: null,
    topics: [],
  };

  componentDidMount() {
    const { match, pipelineTopics } = this.props;
    const { connectorId: streamAppId } = match.params;

    this.setState({ streamAppId, pipelineTopics }, () => {
      this.fetchStreamApp(streamAppId);
    });
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { pipelineTopics: currTopics } = this.props;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ topics: currTopics });
    }

    if (prevConnectorId !== currConnectorId) {
      const streamAppId = currConnectorId;

      this.setState({ streamAppId }, () => {
        this.fetchStreamApp(streamAppId);
      });
    }
  }

  fetchStreamApp = async id => {
    const res = await streamApi.fetchProperty(id);
    const streamApp = get(res, 'data.result', null);

    if (!isEmpty(streamApp)) {
      this.setState({ streamApp });
    }
  };

  getTopics = ({ pipelineTopics, from, to }) => {
    const fromTopic = pipelineTopics.reduce((acc, { name, id }) => {
      return name === from ? [...acc, id] : acc;
    }, []);

    const toTopic = pipelineTopics.reduce((acc, { name, id }) => {
      return name === to ? [...acc, id] : acc;
    }, []);

    return { fromTopic, toTopic };
  };

  handleSave = async ({ name, instances, from, to }) => {
    const { pipelineTopics, graph, updateGraph } = this.props;
    const { fromTopic, toTopic } = this.getTopics({ pipelineTopics, from, to });
    const { streamAppId, streamApp } = this.state;
    const { name: jarName } = streamApp.jarInfo;

    const params = {
      id: streamAppId,
      jarName,
      name,
      instances,
      from: fromTopic,
      to: toTopic,
    };

    const res = await streamApi.updateProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const [streamApp] = graph.filter(g => g.id === streamAppId);
      const [prevFromTopic] = graph.filter(g => g.to.includes(streamAppId));
      const isToUpdate = streamApp.to[0] !== toTopic[0];

      // To topic update
      if (isToUpdate) {
        const currStreamApp = findByGraphId(graph, streamAppId);
        const toUpdate = { ...currStreamApp, to: toTopic };
        updateGraph({ update: toUpdate });
      } else {
        // From topic update
        let currFromTopic = findByGraphId(graph, fromTopic[0]);
        let fromUpdate;

        if (currFromTopic) {
          fromUpdate = [...new Set([...currFromTopic.to, streamAppId])];
        } else {
          if (prevFromTopic) {
            fromUpdate = prevFromTopic.to.filter(t => t !== streamAppId);
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
          streamAppId,
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
    const { connectorId: streamAppId, pipelineId } = match.params;
    const { workerClusterName } = this.props.pipeline;
    const params = {
      id: streamAppId,
      workerClusterName,
    };
    const res = await streamApi.deleteProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const { name: connectorName } = this.state.streamApp;
      toastr.success(`${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${connectorName}`);
      await refreshGraph();

      const path = `/pipelines/edit/${pipelineId}`;
      history.push(path);
    }
  };

  triggerStreamApp = async action => {
    const { streamAppId } = this.state;
    let res;
    if (action === STREAM_APP_ACTIONS.start) {
      res = await streamApi.startStreamApp(streamAppId);
    } else {
      res = await streamApi.stopStreamApp(streamAppId);
    }
    this.handleTriggerStreamAppResponse(action, res);
  };

  handleTriggerStreamAppResponse = (action, res) => {
    const isSuccess = get(res, 'data.isSuccess', false);
    if (!isSuccess) return;

    const { graph, updateGraph } = this.props;
    const { streamAppId } = this.state;
    const state = get(res, 'data.result.state');
    this.setState({ state });

    const currStreamApp = findByGraphId(graph, streamAppId);
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

    const { name, instances, jarInfo, from, to } = streamApp;
    const { name: jarName } = jarInfo;
    const fromTopic = pipelineTopics.find(({ id }) => id === from[0]);
    const toTopic = pipelineTopics.find(({ id }) => id === to[0]);

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
                  <Label htmlFor="name-input">Name</Label>
                  <Field
                    id="name-input"
                    name="name"
                    component={InputField}
                    width="100%"
                    placeholder="Stream app name"
                  />
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
