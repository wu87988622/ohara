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
import toastr from 'toastr';
import PropTypes from 'prop-types';
import { get, debounce } from 'lodash';
import { Form } from 'react-final-form';

import * as connectorApi from 'api/connectorApi';
import * as utils from './customConnectorUtils';
import * as MESSAGES from 'constants/messages';
import * as s from './styles';
import TestConnectionBtn from '../TestConnectionBtn';
import { graph as graphPropType } from 'propTypes/pipeline';
import { validateConnector } from 'api/validateApi';
import { fetchWorker } from 'api/workerApi';

class CustomFinalForm extends React.Component {
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
    const reback = utils.rebackKeys(updatedConfigs);
    this.setState({ configs: reback });
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
    this.setState({ isTestConnectionBtnWorking: true });

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
  }, 500);

  render() {
    const replaceConfigs = utils.replaceKeys(this.state.configs);
    return (
      <Form
        onSubmit={this.handleTestConnection}
        initialValues={replaceConfigs}
        isTestConnectionBtnWorking={this.state.isTestConnectionBtnWorking}
        formProps={{
          defs: this.state.defs,
          configs: this.state.configs,
          topics: this.state.topics,
          state: this.state.state,
          handleChange: this.handleChange,
          handleColumnChange: this.handleColumnChange,
          handleColumnRowDelete: this.handleColumnRowDelete,
          handleColumnRowUp: this.handleColumnRowUp,
          handleColumnRowDown: this.handleColumnRowDown,
        }}
        render={({
          handleSubmit,
          form,
          formProps,
          isTestConnectionBtnWorking,
        }) => {
          return (
            <>
              {utils.renderForm(formProps, form)}
              <s.StyledForm>
                <TestConnectionBtn
                  handleClick={handleSubmit}
                  isWorking={isTestConnectionBtnWorking}
                />
              </s.StyledForm>
            </>
          );
        }}
      />
    );
  }
}

export default CustomFinalForm;
