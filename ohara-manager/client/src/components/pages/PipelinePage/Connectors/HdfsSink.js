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
import { ListLoader } from 'components/common/Loader';
import { Box } from 'components/common/Layout';
import { findByGraphName } from '../pipelineUtils/commonUtils';
import { CONNECTOR_ACTIONS } from 'constants/pipelines';

class HdfsSink extends React.Component {
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
    isLoading: true,
    topics: [],
    configs: null,
    state: null,
    isTestingConfig: false,
  };

  componentDidMount() {
    this.connectorName = this.props.match.params.connectorName;
    this.fetchConnector();
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
      this.fetchConnector();
    }
  }

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(t => t.name) });
  };

  fetchConnector = async () => {
    const res = await connectorApi.fetchConnector(this.connectorName);
    this.setState({ isLoading: false });
    const result = get(res, 'data.result', null);

    if (result) {
      const { settings } = result;
      const { topicKeys } = settings;
      const state = get(result, 'state', null);

      const topicName = utils.getCurrTopicName({
        originals: this.props.globalTopics,
        target: topicKeys,
      });

      const _settings = utils.changeToken({
        values: settings,
        targetToken: '.',
        replaceToken: '_',
      });

      const configs = { ..._settings, topicKeys: topicName };
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
    const { pipelineName } = match.params;
    const res = await connectorApi.deleteConnector(this.connectorName);
    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      const { configs } = this.state;
      toastr.success(
        `${MESSAGES.CONNECTOR_DELETION_SUCCESS} ${configs.connector_name}`,
      );
      await refreshGraph();

      const path = `/pipelines/edit/${pipelineName}`;
      history.push(path);
    }
  };

  triggerConnector = async action => {
    let res;
    if (action === CONNECTOR_ACTIONS.start) {
      res = await connectorApi.startConnector(this.connectorName);
    } else {
      res = await connectorApi.stopConnector(this.connectorName);
    }

    this.handleTriggerConnectorResponse(action, res);
  };

  handleTestConnection = async (e, values) => {
    e.preventDefault();

    const topic = utils.getCurrTopicId({
      originals: this.props.globalTopics,
      target: values.topics,
    });

    const topicKeys = Array.isArray(topic) ? topic : [topic];
    const _values = utils.changeToken({
      values,
      targetToken: '_',
      replaceToken: '.',
    });

    const params = { ..._values, topicKeys };
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

    const { graph, updateGraph } = this.props;
    const state = get(res, 'data.result.state');
    this.setState({ state });

    const currSink = findByGraphName(graph, this.connectorName);
    const update = { ...currSink, state };
    updateGraph({ update });

    if (action === CONNECTOR_ACTIONS.start) {
      if (!isNull(state)) toastr.success(MESSAGES.START_CONNECTOR_SUCCESS);
    }
  };

  handleSave = async values => {
    const { globalTopics, graph, updateGraph } = this.props;

    const topic = utils.getCurrTopicId({
      originals: globalTopics,
      target: values.topicKeys,
    });

    const topicKeys = Array.isArray(topic) ? topic : [topic];
    const _values = utils.changeToken({
      values,
      targetToken: '_',
      replaceToken: '.',
    });

    const params = { ..._values, topicKeys, name: this.connectorName };
    await connectorApi.updateConnector({ name: this.connectorName, params });

    const { sinkProps, update } = utils.getUpdatedTopic({
      currTopicName: topic,
      configs: values,
      originalTopics: globalTopics,
      graph,
      connectorName: this.connectorName,
    });

    updateGraph({ update, dispatcher: { name: 'CONNECTOR' }, ...sinkProps });
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
          <H5Wrapper>HDFS sink connector</H5Wrapper>
          <Controller
            kind="connector"
            connectorName={this.connectorName}
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
                <>
                  <AutoSave
                    save={this.handleSave}
                    updateHasChanges={updateHasChanges}
                  />

                  {utils.renderForm({ parentValues: values, ...formProps })}
                  <TestConfigBtn
                    handleClick={e => this.handleTestConnection(e, values)}
                    isWorking={isTestingConfig}
                  />
                </>
              );
            }}
          />
        )}
      </Box>
    );
  }
}

export default HdfsSink;
