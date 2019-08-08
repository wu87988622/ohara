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
import { Form } from 'react-final-form';
import { get, isString, isNull } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as streamApi from 'api/streamApi';
import * as utils from './connectorUtils';
import Controller from './Controller';
import AutoSave from './AutoSave';
import { TitleWrapper, H5Wrapper, LoaderWrap } from './styles';
import { STREAM_APP_ACTIONS } from 'constants/pipelines';
import { ListLoader } from 'components/common/Loader';
import { Box } from 'components/common/Layout';
import { findByGraphName } from '../pipelineUtils/commonUtils';
import { graph as graphPropType } from 'propTypes/pipeline';

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
    streamApp: null,
    state: null,
    topics: [],
  };

  componentDidMount() {
    this.streamAppName = this.props.match.params.connectorName;
    this.fetchStreamApp(this.streamAppName);
    this.setTopics();
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
      this.fetchStreamApp(currConnectorName);
    }
  }

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(t => t.name) });
  };

  fetchStreamApp = async name => {
    const res = await streamApi.fetchProperty(name);
    this.setState({ isLoading: false });
    const result = get(res, 'data.result', null);

    if (result) {
      const {
        settings,
        definition: { definitions },
      } = result;
      const { topicKeys } = settings;
      const state = get(result, 'state', null);
      const topicName = get(topicKeys, '[0].name', '');

      const _settings = utils.changeToken({
        values: settings,
        targetToken: '.',
        replaceToken: '_',
      });

      const configs = { ..._settings, topicKeys: topicName };
      this.setState({ configs, state, defs: definitions });
    }
  };

  handleSave = async ({ instances, from, to }) => {
    const { graph, updateGraph } = this.props;
    let fromTopic = isString(from) ? [from] : from;
    let toTopic = isString(to) ? [to] : to;

    const params = {
      name: this.streamAppName,
      instances,
      from: fromTopic,
      to: toTopic,
    };

    const res = await streamApi.updateProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const [streamApp] = graph.filter(g => g.name === this.streamAppName);
      const [prevFromTopic] = graph.filter(g =>
        g.to.includes(this.streamAppName),
      );
      const isToUpdate = streamApp.to[0] !== toTopic[0];

      // To topic update
      if (isToUpdate) {
        const currStreamApp = findByGraphName(graph, this.streamAppName);
        const toUpdate = { ...currStreamApp, to: toTopic };
        updateGraph({ update: toUpdate, dispatcher: { name: 'STREAM_APP' } });
      } else {
        // From topic update
        let currFromTopic = findByGraphName(graph, fromTopic[0]);
        let fromUpdate;

        if (currFromTopic) {
          fromUpdate = [...new Set([...currFromTopic.to, this.streamAppName])];
        } else {
          if (prevFromTopic) {
            fromUpdate = prevFromTopic.to.filter(t => t !== this.streamAppName);
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
          dispatcher: { name: 'STREAM_APP' },
          update,
          isFromTopic: true,
          streamAppName: this.streamAppName,
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
    const { match, refreshGraph, history } = this.props;
    const { pipelineName } = match.params;

    const res = await streamApi.deleteProperty(this.streamAppName);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(
        `${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${this.streamAppName}`,
      );
      await refreshGraph();

      const path = `/pipelines/edit/${pipelineName}`;
      history.push(path);
    }
  };

  triggerStreamApp = async action => {
    let res;
    if (action === STREAM_APP_ACTIONS.start) {
      res = await streamApi.startStreamApp(this.streamAppName);
    } else {
      res = await streamApi.stopStreamApp(this.streamAppName);
    }
    this.handleTriggerStreamAppResponse(action, res);
  };

  handleTriggerStreamAppResponse = (action, res) => {
    const isSuccess = get(res, 'data.isSuccess', false);
    if (!isSuccess) return;

    const { graph, updateGraph } = this.props;
    const state = get(res, 'data.result.state');
    this.setState({ state });

    const currStreamApp = findByGraphName(graph, this.streamAppName);
    const update = { ...currStreamApp, state };
    updateGraph({ update, dispatcher: { name: 'STREAM_APP' } });

    if (action === STREAM_APP_ACTIONS.start) {
      if (!isNull(state)) toastr.success(MESSAGES.STREAM_APP_STOP_SUCCESS);
    }
  };

  render() {
    const { state, configs, isLoading, topics, defs } = this.state;
    const { updateHasChanges } = this.props;

    if (!configs) return null;

    const formData = utils.getRenderData({
      defs,
      configs,
      state,
    });

    const initialValues = formData.reduce((acc, cur) => {
      acc[cur.key] = cur.displayValue;
      return acc;
    }, {});

    const formProps = {
      formData,
      topics,
      handleColumnChange: this.handleColumnChange,
      handleColumnRowDelete: this.handleColumnRowDelete,
      handleColumnRowUp: this.handleColumnRowUp,
      handleColumnRowDown: this.handleColumnRowDown,
    };

    return (
      <Box>
        <TitleWrapper>
          <H5Wrapper>FTP source connector</H5Wrapper>
          <Controller
            kind="connector"
            connectorName={this.streamAppName}
            onStart={this.handleStartStreamApp}
            onStop={this.handleStopStreamApp}
            onDelete={this.handleDeleteStreamApp}
          />
        </TitleWrapper>
        {isLoading ? (
          <LoaderWrap>
            <ListLoader />
          </LoaderWrap>
        ) : (
          <Form
            onSubmit={this.handleSave}
            initialValues={initialValues}
            render={({ values }) => {
              return (
                <form>
                  <AutoSave
                    save={this.handleSave}
                    updateHasChanges={updateHasChanges}
                  />

                  {utils.renderForm({ parentValues: values, ...formProps })}
                </form>
              );
            }}
          />
        )}
      </Box>
    );
  }
}

export default StreamApp;
