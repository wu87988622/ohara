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
import styled from 'styled-components';
import toastr from 'toastr';
import { get, isEmpty, isNull, debounce } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as connectorApi from 'api/connectorApi';
import { validateConnector } from 'api/validateApi';
import * as s from './styles';
import * as utils from './CustomConnector/customConnectorUtils';
import Controller from './Controller';

import { findByGraphId } from '../pipelineUtils/commonUtils';

import { fetchWorker } from 'api/workerApi';
import TestConnectionBtn from './CustomConnector/TestConnectionBtn';
import { CONNECTOR_STATES, CONNECTOR_ACTIONS } from 'constants/pipelines';
import { graphPropType } from 'propTypes/pipeline';

class FtpSource extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    loadGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    match: PropTypes.shape({
      params: PropTypes.object,
    }).isRequired,
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    history: PropTypes.shape({
      push: PropTypes.func.isRequired,
    }).isRequired,
    pipelineTopics: PropTypes.array.isRequired,
    isPipelineRunning: PropTypes.bool.isRequired,
    globalTopics: PropTypes.array.isRequired,
    workerClusterName: PropTypes.string.isRequired,
  };

  state = {
    defs: [],
    topics: [],
    configs: null,
    state: null,
    isTestConnectionBtnWorking: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { hasChanges, pipelineTopics: currTopics } = this.props;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ writeTopics: currTopics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchData();
    }

    if (hasChanges) {
      this.save();
    }
  }

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(t => t.name) });
  };

  fetchData = async () => {
    // We need to get the custom connector's meta data first
    await this.fetchWorker();

    // After the form is rendered, let's fetch connector data and override the default values from meta data
    this.fetchConnector();
    this.setTopics();
  };

  fetchWorker = async () => {
    const res = await fetchWorker(this.props.workerClusterName);
    const worker = get(res, 'data.result', null);
    this.setState({ isLoading: false });

    if (worker) {
      const { defs, configs } = utils.getMetadata(this.props, worker);
      this.setState({ defs, configs });
    }
  };

  fetchConnector = async () => {
    const { connectorId } = this.props.match.params;
    const res = await connectorApi.fetchConnector(connectorId);
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
    const updatedConfigs = utils.removeColumn({ configs, currRow });
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

  handleTestConnection = async e => {
    e.preventDefault();
    this.setState({ isTestConnectionBtnWorking: true });

    // const topics = this.state.topic
    const topicId = utils.getCurrTopicId({
      originals: this.props.globalTopics,
      target: this.state.topics[0],
    });

    const params = { ...this.state.configs, topics: topicId };
    const res = await validateConnector(params);
    this.setState({ isTestConnectionBtnWorking: false });

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
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
    const sourceId = get(match, 'params.connectorId', null);
    let res;
    if (action === CONNECTOR_ACTIONS.start) {
      res = await connectorApi.startConnector(sourceId);
    } else {
      res = await connectorApi.stopConnector(sourceId);
    }

    this.handleTriggerConnectorResponse(action, res);
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

    const params = { ...configs, topics: topicId };

    await connectorApi.updateConnector({ id: connectorId, params });
    updateHasChanges(false);

    const update = utils.getUpdatedTopic({
      graph,
      connectorId,
      configs,
      originalTopics: globalTopics,
    });

    updateGraph({ update });
  }, 1000);

  render() {
    const {
      defs,
      configs,
      topics,
      state,
      isTestConnectionBtnWorking,
    } = this.state;

    const formProps = {
      defs,
      configs,
      topics,
      state,
      handleChange: this.handleChange,
      handleColumnChange: this.handleColumnChange,
      handleColumnRowDelete: this.handleColumnRowDelete,
      handleColumnRowUp: this.handleColumnRowUp,
      handleColumnRowDown: this.handleColumnRowDown,
    };
    return (
      <React.Fragment>
        <s.BoxWrapper padding="25px 0 0 0">
          <s.TitleWrapper margin="0 25px 30px">
            <s.H5Wrapper>FTP source connector</s.H5Wrapper>
            <Controller
              kind="connector"
              onStart={this.handleStartConnector}
              onStop={this.handleStopConnector}
              onDelete={this.handleDeleteConnector}
            />
          </s.TitleWrapper>
          {utils.renderForm(formProps)}
          <s.StyledForm>
            <TestConnectionBtn
              handleClick={this.handleTestConnection}
              isWorking={isTestConnectionBtnWorking}
            />
          </s.StyledForm>
        </s.BoxWrapper>
      </React.Fragment>
    );
  }
}

export default FtpSource;
