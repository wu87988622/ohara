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
import { get, isString, isNull, isEmpty } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as streamApi from 'api/streamApi';
import * as pipelineApi from 'api/pipelineApi';
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
    pipeline: PropTypes.shape({
      name: PropTypes.string.isRequired,
      flows: PropTypes.arrayOf(
        PropTypes.shape({
          from: PropTypes.object,
          to: PropTypes.arrayOf(PropTypes.object),
        }),
      ).isRequired,
    }).isRequired,
    updateGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    pipelineTopics: PropTypes.array.isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
  };

  selectMaps = {
    fromTopics: 'currFromTopic',
  };

  state = {
    streamApp: null,
    state: null,
    topics: [],
    from: null,
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
      const topics = currTopics.map(currTopic => currTopic.name);
      this.setState({ topics });
    }

    if (prevConnectorName !== currConnectorName) {
      this.fetchStreamApp(currConnectorName);
    }
  }

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(topic => topic.name) });
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
      const { from, to } = settings;
      const state = get(result, 'state', null);
      const fromTopic = get(from, '[0].name', '');
      const toTopic = get(to, '[0].name', '');

      const _settings = utils.changeToken({
        values: settings,
        targetToken: '.',
        replaceToken: '_',
      });

      const configs = { ..._settings, from: fromTopic, to: toTopic };
      this.setState({
        configs,
        state,
        defs: definitions,
        from: fromTopic,
      });
    }
  };

  handleSave = async values => {
    const { instances, from, to } = values;

    let isFromUpdate = false;
    if (from !== this.state.from) {
      if (from !== null) isFromUpdate = true;
      this.setState({ from });
    }

    const { graph, updateGraph } = this.props;
    let fromTopic = isString(from) ? [from] : from;
    let toTopic = isString(to) ? [to] : to;

    if (
      (fromTopic && fromTopic[0] === 'Please select...') ||
      isNull(fromTopic)
    ) {
      fromTopic = [];
    }

    if ((toTopic && toTopic[0] === 'Please select...') || isNull(toTopic)) {
      toTopic = [];
    }

    const fromKey = isEmpty(fromTopic)
      ? fromTopic
      : [{ group: 'default', name: fromTopic[0] }];

    const toKey = isEmpty(toTopic)
      ? toTopic
      : [{ group: 'default', name: toTopic[0] }];

    const params = {
      ...values,
      name: this.streamAppName,
      instances,
      from: fromKey,
      to: toKey,
    };

    const res = await streamApi.updateProperty(params);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const [prevFromTopic] = graph.filter(g =>
        g.to.includes(this.streamAppName),
      );

      // To topic update
      if (!isFromUpdate) {
        const currStreamApp = findByGraphName(graph, this.streamAppName);
        const toUpdate = { ...currStreamApp, to: toTopic };
        updateGraph({ update: toUpdate, dispatcher: { name: 'STREAM_APP' } });
      } else {
        // From topic update
        let currFromTopic = findByGraphName(graph, from);

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
    const { refreshGraph, history, pipeline } = this.props;
    const { name: pipelineName, flows } = pipeline;

    if (this.state.state) {
      toastr.error(
        `The connector is running! Please stop the connector first before deleting`,
      );

      return;
    }

    const connectorResponse = await streamApi.deleteProperty(
      this.streamAppName,
    );

    const connectorHasDeleted = get(connectorResponse, 'data.isSuccess', false);

    const updatedFlows = flows.filter(
      flow => flow.from.name !== this.streamAppName,
    );

    const pipelineResponse = await pipelineApi.updatePipeline({
      name: pipelineName,
      params: {
        name: pipelineName,
        flows: updatedFlows,
      },
    });

    const pipelineHasUpdated = get(pipelineResponse, 'data.isSuccess', false);

    if (connectorHasDeleted && pipelineHasUpdated) {
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

    const isSuccess = get(res, 'data.isSuccess', false);
    this.handleTriggerConnectorResponse(action, isSuccess);
  };

  handleTriggerConnectorResponse = async (action, isSuccess) => {
    if (!isSuccess) return;

    const response = await streamApi.fetchProperty(this.streamAppName);
    const state = get(response, 'data.result.state', null);
    const { graph, updateGraph } = this.props;

    this.setState({ state });
    const currStreamApp = findByGraphName(graph, this.streamAppName);
    const update = { ...currStreamApp, state };
    updateGraph({ update, dispatcher: { name: 'STREAM_APP' } });

    if (action === STREAM_APP_ACTIONS.start) {
      if (!isNull(state)) toastr.success(MESSAGES.STREAM_APP_START_SUCCESS);
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
          <H5Wrapper>Stream app</H5Wrapper>
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
