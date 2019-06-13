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
import { get, isNull } from 'lodash';

import * as MESSAGES from 'constants/messages';
import * as connectorApi from 'api/connectorApi';
import * as utils from './connectorUtils';
import * as types from 'propTypes/pipeline';
import Controller from './Controller';
import TestConfigBtn from './TestConfigBtn';
import AutoSave from './AutoSave';
import { TitleWrapper, H5Wrapper, LoaderWrap } from './styles';
import { validateConnector } from 'api/validateApi';
import { ListLoader } from 'common/Loader';
import { Box } from 'common/Layout';
import { findByGraphId } from '../pipelineUtils/commonUtils';
import { CONNECTOR_ACTIONS } from 'constants/pipelines';

class FtpSource extends React.Component {
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
    defs: [],
    topics: [],
    configs: null,
    state: null,
    isTestingConfig: false,
  };

  componentDidMount() {
    this.fetchConnector();
    this.setTopics();
  }

  componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { pipelineTopics: currTopics } = this.props;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      const topics = currTopics.map(currTopic => currTopic.name);
      this.setState({ topics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchConnector();
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
      const { settings } = result;
      const { topics } = settings;
      const state = get(result, 'result.settings.state', null);

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

  handleColumnChange = update => {
    const { configs } = this.state;
    const updatedConfigs = utils.addColumn({ configs, update });
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowDelete = update => {
    const { configs } = this.state;
    const updatedConfigs = utils.deleteColumnRow({ configs, update });
    this.updateComponent(updatedConfigs);
  };

  handleColumnRowUp = (e, update) => {
    e.preventDefault();
    const { configs } = this.state;
    const updatedConfigs = utils.moveColumnRowUp({ configs, update });

    if (updatedConfigs) this.updateComponent(updatedConfigs);
  };

  handleColumnRowDown = (e, update) => {
    e.preventDefault();
    const { configs } = this.state;
    const updatedConfigs = utils.moveColumnRowDown({ configs, update });

    if (updatedConfigs) this.updateComponent(updatedConfigs);
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
      const { configs } = this.state;
      toastr.success(
        `${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${configs.connector_name}`,
      );
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
    this.setState({ isTestingConfig: true });
    const res = await validateConnector(params);
    this.setState({ isTestingConfig: false });
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
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
      if (!isNull(state)) toastr.success(MESSAGES.START_CONNECTOR_SUCCESS);
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

    const { sinkProps, update } = utils.getUpdatedTopic({
      currTopicId: topic,
      configs: values,
      originalTopics: globalTopics,
      graph,
      connectorId,
    });

    updateGraph({ update, ...sinkProps });
  };

  render() {
    const { state, configs, isLoading, topics, isTestingConfig } = this.state;
    const { defs, updateHasChanges } = this.props;

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
            onStart={this.handleStartConnector}
            onStop={this.handleStopConnector}
            onDelete={this.handleDeleteConnector}
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
                  <TestConfigBtn
                    handleClick={e => this.handleTestConnection(e, values)}
                    isWorking={isTestingConfig}
                  />
                </form>
              );
            }}
          />
        )}
      </Box>
    );
  }
}

export default FtpSource;
