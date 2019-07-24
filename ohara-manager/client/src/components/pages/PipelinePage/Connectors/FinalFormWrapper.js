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
import { get, isUndefined } from 'lodash';
import { Form } from 'react-final-form';

import * as connectorApi from 'api/connectorApi';
import * as utils from './connectorUtils';
import * as MESSAGES from 'constants/messages';
import * as types from 'propTypes/pipeline';
import AutoSave from './AutoSave';
import TestConnectionBtn from './TestConnectionBtn';
import { validateConnector } from 'api/validateApi';

class FinalFormWrapper extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    pipelineTopics: PropTypes.array.isRequired,
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
    defs: [],
    topics: [],
    configs: null,
    state: null,
    isTestConnectionBtnWorking: false,
  };

  componentDidMount() {
    this.fetchConnector();
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { pipelineTopics: currTopics } = this.props;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ writeTopics: currTopics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchData();
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

      const _settings = utils.changeToken({
        values: settings,
        targetToken: '.',
        replaceToken: '_',
      });

      const configs = { ..._settings, topics: topicName };
      this.setState({ configs, state });
    }
  };

  updateComponent = updatedConfigs => {
    this.props.updateHasChanges(true);
    this.setState({ configs: updatedConfigs });
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
    if (isUndefined(updatedConfigs)) {
      return;
    }
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowDown = (e, order) => {
    e.preventDefault();
    const { configs } = this.state;
    const updatedConfigs = utils.moveColumnRowDown({ configs, order });
    if (isUndefined(updatedConfigs)) {
      return;
    }
    this.updateComponent(updatedConfigs);
  };

  handleTestConnection = async (e, values) => {
    e.preventDefault();

    const topic = utils.getCurrTopicId({
      originals: this.props.globalTopics,
      target: values.topics,
    });

    const topics = Array.isArray(topic) ? topic : [topic];
    const _values = utils.changeToken({
      values,
      targetToken: '_',
      replaceToken: '.',
    });

    const params = { ..._values, topics };
    this.setState({ isTestConnectionBtnWorking: true });
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

  handleSave = async values => {
    const { match, globalTopics, graph, updateGraph } = this.props;
    const { connectorId } = match.params;

    const topic = utils.getCurrTopicId({
      originals: globalTopics,
      target: values.topics,
    });

    const topics = Array.isArray(topic) ? topic : [topic];
    const _values = utils.changeToken({
      values,
      targetToken: '_',
      replaceToken: '.',
    });

    const params = { ..._values, topics };
    await connectorApi.updateConnector({ id: connectorId, params });

    const { update, connectorProps } = utils.getUpdatedTopic({
      currTopicId: topic,
      configs: values,
      originalTopics: globalTopics,
      graph,
      connectorId,
    });

    updateGraph({ update, ...connectorProps });
  };

  render() {
    const { state, configs, topics, isTestConnectionBtnWorking } = this.state;

    const { defs, updateHasChanges } = this.props;

    if (!configs) return null;

    const formData = utils.getRenderData({
      defs,
      topics,
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
      handleChange: this.handleChange,
      handleColumnRowDelete: this.handleColumnRowDelete,
      handleColumnRowUp: this.handleColumnRowUp,
      handleColumnRowDown: this.handleColumnRowDown,
    };

    return (
      <Form
        onSubmit={this.handleSave}
        initialValues={initialValues}
        formProps={formProps}
        render={({ values }) => {
          return (
            <form>
              <AutoSave
                save={this.handleSave}
                updateHasChanges={updateHasChanges}
                hasToken
              />

              {utils.renderForm(formProps)}
              <TestConnectionBtn
                handleClick={e => this.handleTestConnection(e, values)}
                isWorking={isTestConnectionBtnWorking}
              />
            </form>
          );
        }}
      />
    );
  }
}

export default FinalFormWrapper;
