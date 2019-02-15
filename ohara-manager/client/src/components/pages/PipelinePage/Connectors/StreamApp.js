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
import * as streamAppApi from 'api/streamAppApi';
import * as _ from 'utils/commonUtils';
import Controller from './Controller';
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
    updateHasChanges: PropTypes.func.isRequired,
    topics: PropTypes.array.isRequired,
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
    const { match, topics } = this.props;
    const streamAppId = get(match, 'params.connectorId', null);

    this.setState({ streamAppId, topics }, () => {
      this.fetchStreamApp(streamAppId);
    });
  }

  fetchStreamApp = async id => {
    const res = await streamAppApi.fetchProperty(id);
    const streamApp = get(res, 'data.result', null);

    if (!isEmpty(streamApp)) {
      this.setState({ streamApp });
    }
  };

  handleSave = async ({ name, instances, fromTopic, toTopic }) => {
    const { topics, updateHasChanges, graph, updateGraph } = this.props;
    const { streamAppId } = this.state;

    const fromTopics = topics.reduce((acc, { name, id }) => {
      return name === fromTopic ? [...acc, id] : acc;
    }, []);

    const toTopics = topics.reduce((acc, { name, id }) => {
      return name === toTopic ? [...acc, id] : acc;
    }, []);

    const params = {
      id: streamAppId,
      name,
      instances,
      fromTopics,
      toTopics,
    };

    updateHasChanges(true);
    const res = await streamAppApi.updateProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const [streamApp] = graph.filter(g => g.id === streamAppId);
      const [prevFromTopic] = graph.filter(g => g.to.includes(streamAppId));
      const isToUpdate = streamApp.to[0] !== toTopics[0];

      if (isToUpdate) {
        const currStreamApp = findByGraphId(graph, streamAppId);
        const toUpdate = { ...currStreamApp, to: toTopics };
        updateGraph({ update: toUpdate });
      } else {
        let currTopic = findByGraphId(graph, fromTopics[0]);
        let fromUpdateTo;

        if (currTopic) {
          fromUpdateTo = [...new Set([...currTopic.to, streamAppId])];
        } else {
          if (prevFromTopic) {
            fromUpdateTo = prevFromTopic.to.filter(t => t !== streamAppId);
          } else {
            fromUpdateTo = [];
          }
          currTopic = prevFromTopic;
        }

        const fromUpdate = {
          ...currTopic,
          to: fromUpdateTo,
        };

        updateGraph({
          update: fromUpdate,
          isStreamAppFromUpdate: true,
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

  handleDeleteStreamApp = async () => {
    const { refreshGraph } = this.props;
    const { streamAppId } = this.state;
    const res = await streamAppApi.del(streamAppId);
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
      res = await streamAppApi.start(streamAppId);
    } else {
      res = await streamAppApi.stop(streamAppId);
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
    const { updateHasChanges } = this.props;
    const { topics, streamApp } = this.state;

    if (!streamApp) return null;

    const { name, instances, jarName, fromTopics, toTopics } = streamApp;
    const from = topics.find(({ id }) => id === fromTopics[0]);
    const to = topics.find(({ id }) => id === toTopics[0]);

    const initialValues = {
      name: _.isEmptyStr(name) ? 'Untitled stream app' : name,
      instances: `${instances}`,
      fromTopic: !isEmpty(from) ? from.name : null,
      toTopic: !isEmpty(to) ? to.name : null,
    };

    return (
      <React.Fragment>
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
                    placeholder="select a topic ..."
                    isObject
                    clearable
                  />
                </s.FormCol>
                <s.FormCol width="50%">
                  <Label>To topic</Label>
                  <Field
                    name="toTopic"
                    component={SelectField}
                    list={topics}
                    width="100%"
                    placeholder="select a topic ..."
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
      </React.Fragment>
    );
  }
}

export default StreamApp;
