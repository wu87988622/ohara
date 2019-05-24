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
import { get, debounce, isUndefined } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as connectorApi from 'api/connectorApi';
import * as utils from './connectorUtils';
import * as s from './styles';
import * as types from 'propTypes/pipeline';
import Controller from './Controller';
import TestConnectionBtn from './TestConnectionBtn';
import { validateConnector } from 'api/validateApi';
import { ListLoader } from 'common/Loader';
import { Box } from 'common/Layout';
import { findByGraphId } from '../pipelineUtils/commonUtils';
import { CONNECTOR_STATES, CONNECTOR_ACTIONS } from 'constants/pipelines';

class HdfsSink extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    loadGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    pipelineTopics: PropTypes.array.isRequired,
    isPipelineRunning: PropTypes.bool.isRequired,
    globalTopics: PropTypes.arrayOf(types.topic).isRequired,
    defs: PropTypes.arrayOf(types.definition),
    graph: PropTypes.arrayOf(types.graph).isRequired,
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
    }).isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
  };

  state = {
    isLoading: true,
    topics: [],
    configs: null,
    state: null,
    isTestConnectionBtnWorking: false,
  };

  componentDidMount() {
    this.fetchConnector();
    this.setTopics();
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { hasChanges, pipelineTopics: currTopics } = this.props;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      const topics = currTopics.map(currTopic => currTopic.name);
      this.setState({ topics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchConnector();
    }

    if (hasChanges) {
      this.save();
    }
  }

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(t => t.name) });
  };

  fetchConnector = async () => {
    const { connectorId } = this.props.match.params;
    const res = await connectorApi.fetchConnector(connectorId);
    this.setState({ isLoading: false });
    const result = get(res, 'data.result', null);

    if (result) {
      const { settings, state } = result;
      const { topics } = settings;

      const topicName = utils.getCurrTopicName({
        originals: this.props.globalTopics,
        target: topics,
      });

      const configs = { ...settings, topics: topicName };
      this.setState({ configs, state });
    }
  };

  updateComponent = updatedConfigs => {
    this.props.updateHasChanges(true);
    this.setState({ configs: updatedConfigs });
  };

  handleChange = ({ target }) => {
    const { configs } = this.state;
    const updatedConfigs = utils.updateConfigs({ configs, target });
    this.updateComponent(updatedConfigs);
  };

  handleColumnChange = newColumn => {
    const { configs } = this.state;
    const updatedConfigs = utils.addColumn({ configs, newColumn });
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowDelete = currRow => {
    const { configs } = this.state;
    const updatedConfigs = utils.deleteColumnRow({ configs, currRow });
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowUp = (e, order) => {
    e.preventDefault();
    const { configs } = this.state;
    const updatedConfigs = utils.moveColumnRowUp({ configs, order });
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowDown = (e, order) => {
    e.preventDefault();
    const { configs } = this.state;
    const updatedConfigs = utils.moveColumnRowDown({ configs, order });
    this.updateComponent(updatedConfigs);
  };

  handleStartConnector = async () => {
    await this.triggerConnector(CONNECTOR_ACTIONS.start);
  };

  handleStopConnector = async () => {
    await this.triggerConnector(CONNECTOR_ACTIONS.stop);
  };

  handleDeleteConnector = async () => {
    const { match, refreshGraph, history } = this.props;
    const { connectorId, pipelineId } = match.params;
    const res = await connectorApi.deleteConnector(connectorId);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const { name: connectorName } = this.state;
      toastr.success(`${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${connectorName}`);
      await refreshGraph();

      const path = `/pipelines/edit/${pipelineId}`;
      history.push(path);
    }
  };

  triggerConnector = async action => {
    const { match } = this.props;
    const sinkId = get(match, 'params.connectorId', null);
    let res;
    if (action === CONNECTOR_ACTIONS.start) {
      res = await connectorApi.startConnector(sinkId);
    } else {
      res = await connectorApi.stopConnector(sinkId);
    }

    this.handleTriggerConnectorResponse(action, res);
  };

  handleTestConnection = async e => {
    e.preventDefault();
    this.setState({ isTestConnectionBtnWorking: true });

    // const topics = this.state.topic
    const topicId = utils.getCurrTopicId({
      originals: this.props.globalTopics,
      target: this.state.topics[0],
    });

    const params = { ...this.state.configs, topics: [topicId] };
    const res = await validateConnector(params);
    this.setState({ isTestConnectionBtnWorking: false });

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
  };

  updateIsTestConnectionBtnWorking = update => {
    this.setState({ isTestConnectionBtnWorking: update });
  };

  handleTriggerConnectorResponse = (action, res) => {
    const isSuccess = get(res, 'data.isSuccess', false);
    if (!isSuccess) return;

    const { match, graph, updateGraph } = this.props;
    const sinkId = get(match, 'params.connectorId', null);
    const state = get(res, 'data.result.state');
    this.setState({ state });
    const currSink = findByGraphId(graph, sinkId);
    const update = { ...currSink, state };
    updateGraph({ update });

    if (action === CONNECTOR_ACTIONS.start) {
      if (state === CONNECTOR_STATES.running) {
        toastr.success(MESSAGES.START_CONNECTOR_SUCCESS);
      } else {
        toastr.error(MESSAGES.CANNOT_START_CONNECTOR_ERROR);
      }
    }
  };

  save = debounce(async () => {
    const {
      updateHasChanges,
      match,
      graph,
      updateGraph,
      globalTopics,
    } = this.props;
    const { configs } = this.state;
    const { connectorId } = match.params;

    const topicId = utils.getCurrTopicId({
      originals: globalTopics,
      target: configs.topics,
    });

    const topics = isUndefined(topicId) ? [] : [topicId];
    const params = { ...configs, topics };

    await connectorApi.updateConnector({ id: connectorId, params });
    updateHasChanges(false);

    const { sinkProps, update } = utils.getUpdatedTopic({
      currTopicId: topicId,
      graph,
      configs,
      connectorId,
      originalTopics: globalTopics,
    });

    updateGraph({ update, ...sinkProps });
  }, 1000);

  render() {
    const {
      state,
      configs,
      isLoading,
      topics,
      isTestConnectionBtnWorking,
    } = this.state;
    const { defs } = this.props;

    if (!configs) return null;

    const formProps = {
      defs,
      topics,
      configs,
      state,
      handleChange: this.handleChange,
      handleColumnChange: this.handleColumnChange,
      handleColumnRowDelete: this.handleColumnRowDelete,
      handleColumnRowUp: this.handleColumnRowUp,
      handleColumnRowDown: this.handleColumnRowDown,
    };

    return (
      <Box>
        <s.TitleWrapper>
          <s.H5Wrapper>HDFS sink connector</s.H5Wrapper>
          <Controller
            kind="connector"
            onStart={this.handleStartConnector}
            onStop={this.handleStopConnector}
            onDelete={this.handleDeleteConnector}
          />
        </s.TitleWrapper>

        {isLoading ? (
          <s.LoaderWrap>
            <ListLoader />
          </s.LoaderWrap>
        ) : (
          <>
            {utils.renderForm(formProps)}
            <TestConnectionBtn
              handleClick={this.handleTestConnection}
              isWorking={isTestConnectionBtnWorking}
            />
          </>
        )}
      </Box>
    );
  }
}

export default HdfsSink;
