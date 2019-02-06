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
import { map, get, isEmpty } from 'lodash';
import { Field, Form } from 'react-final-form';
import toastr from 'toastr';

import * as MESSAGES from 'constants/messages';
import * as streamAppApis from 'apis/streamAppApis';
import Controller from './Controller';
import { fetchTopics } from 'apis/topicApis';
import { STREAM_APP_STATES, STREAM_APP_ACTIONS } from 'constants/pipelines';
import { Box } from 'common/Layout';
import { Label } from 'common/Form';
import { InputField, SelectField, AutoSave } from 'common/FormFields';
import { findByGraphId } from '../pipelineUtils/commonUtils';

import * as s from './Styles';

class StreamApp extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
    graph: PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.string,
        id: PropTypes.string,
        isActive: PropTypes.bool,
        isExact: PropTypes.bool,
        icon: PropTypes.string,
      }),
    ).isRequired,
    updateGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
  };

  state = {
    streamAppId: null,
    streamApp: null,
    state: null,
    topics: [],
  };

  componentDidMount() {
    const { match } = this.props;
    const streamAppId = get(match, 'params.connectorId', null);
    this.setState({ streamAppId }, () => {
      this.fetchData();
    });
  }

  fetchData = () => {
    const { streamAppId } = this.state;
    const fetchTopicsPromise = this.fetchTopics();
    const fetchStreamAppPromise = this.fetchStreamApp(streamAppId);

    Promise.all([fetchTopicsPromise, fetchStreamAppPromise]);
  };

  fetchTopics = async () => {
    const res = await fetchTopics();
    const isSuccess = get(res, 'data.isSuccess', null);
    if (isSuccess) {
      const topics = get(res, 'data.result', []);
      this.setState({ topics: map(topics, 'name') });
    }
  };

  fetchStreamApp = async id => {
    const res = await streamAppApis.fetchProperty(id);
    const isSuccess = get(res, 'data.isSuccess', null);

    if (isSuccess) {
      this.setState({ streamApp: res.data.result });
    }
  };

  handleSave = async values => {
    const { streamAppId } = this.state;
    const params = {
      id: streamAppId,
      name: values.name,
      instances: values.instances,
      fromTopics: values.fromTopic ? [values.fromTopic] : [],
      toTopics: values.toTopic ? [values.toTopic] : [],
    };

    const res = await streamAppApis.updateProperty(params);

    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      const { graph, updateGraph } = this.props;
      const streamAppId = params.id;
      const currStreamApp = findByGraphId(graph, streamAppId);
      const update = { ...currStreamApp, ...params };
      updateGraph(update, currStreamApp.id);
      toastr.success(MESSAGES.AUTO_SAVE_SUCCESS);
    }
  };

  handleStartStreamApp = async () => {
    await this.triggerStreamApp(STREAM_APP_ACTIONS.start);
  };

  handleStopStreamApp = async () => {
    await this.triggerStreamApp(STREAM_APP_ACTIONS.stop);
  };

  handleDeleteStreamApp = async () => {
    const { refreshGraph } = this.props;
    const { streamAppId } = this.state;
    const res = await streamAppApis.del(streamAppId);
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_DELETION_SUCCESS);
      refreshGraph();
    }
  };

  triggerStreamApp = async action => {
    const { streamAppId } = this.state;
    let res;
    if (action === STREAM_APP_ACTIONS.start) {
      res = await streamAppApis.start(streamAppId);
    } else {
      res = await streamAppApis.stop(streamAppId);
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
    updateGraph(update, currStreamApp.id);

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
    const { topics, streamApp } = this.state;
    if (!streamApp) return null;

    const { name, instances, jarName, fromTopics, toTopics } = streamApp;
    const initialValues = {
      name,
      instances: `${instances}`,
      fromTopic: !isEmpty(fromTopics) ? fromTopics[0] : null,
      toTopic: !isEmpty(toTopics) ? toTopics[0] : null,
    };

    return (
      <React.Fragment>
        <Form
          onSubmit={this.handleSave}
          initialValues={initialValues}
          render={() => (
            <Box>
              <AutoSave save={this.handleSave} />
              <s.TitleWrapper>
                <s.H5Wrapper>Stream app</s.H5Wrapper>
                <Controller
                  kind="stream app"
                  onStart={this.handleStartStreamApp}
                  onStop={this.handleStopStreamApp}
                  onDelete={this.handleDeleteStreamApp}
                />
              </s.TitleWrapper>
              <s.FormRow>
                <s.FormCol width="70%">
                  <Label>Name</Label>
                  <Field
                    name="name"
                    component={InputField}
                    width="100%"
                    placeholder="Stream app name"
                    data-testid="name-input"
                  />
                </s.FormCol>
                <s.FormCol width="30%">
                  <Label>Instances</Label>
                  <Field
                    name="instances"
                    component={InputField}
                    type="number"
                    min={1}
                    max={100}
                    width="100%"
                    placeholder="1"
                    data-testid="instances-input"
                  />
                </s.FormCol>
              </s.FormRow>
              <s.FormRow>
                <s.FormCol width="50%">
                  <Label>From topic</Label>
                  <Field
                    name="fromTopic"
                    component={SelectField}
                    list={topics}
                    width="100%"
                    placeholder="Select topic ..."
                    data-testid="from-topic-select"
                  />
                </s.FormCol>
                <s.FormCol width="50%">
                  <Label>To topic</Label>
                  <Field
                    name="toTopic"
                    component={SelectField}
                    list={topics}
                    width="100%"
                    placeholder="Select topic ..."
                    data-testid="to-topic-select"
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
                  <s.ViewTopologyBtn
                    text="View topology"
                    data-testid="view-topology-button"
                    disabled
                  />
                </s.FormCol>
              </s.FormRow>
            </Box>
          )}
        />
      </React.Fragment>
    );
  }
}

export default StreamApp;
