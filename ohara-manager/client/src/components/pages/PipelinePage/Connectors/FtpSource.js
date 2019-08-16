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
import * as pipelineApi from 'api/pipelineApi';
import Controller from './Controller';
import TestConfigBtn from './TestConfigBtn';
import AutoSave from './AutoSave';
import { TitleWrapper, H5Wrapper, LoaderWrap } from './styles';
import { validateConnector } from 'api/validateApi';
import { ListLoader } from 'components/common/Loader';
import { Box } from 'components/common/Layout';
import { findByGraphName } from '../pipelineUtils/commonUtils';
import { CONNECTOR_ACTIONS } from 'constants/pipelines';

class FtpSource extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    refreshGraph: PropTypes.func.isRequired,
    pipelineTopics: PropTypes.array.isRequired,
    globalTopics: PropTypes.arrayOf(types.topic).isRequired,
    defs: PropTypes.arrayOf(types.definition),
    graph: PropTypes.arrayOf(types.graph).isRequired,
    pipeline: PropTypes.shape({
      name: PropTypes.string.isRequired,
      flows: PropTypes.arrayOf(
        PropTypes.shape({
          from: PropTypes.object,
          to: PropTypes.arrayOf(PropTypes.object),
        }),
      ).isRequired,
    }).isRequired,
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
      this.connectorName = currConnectorName;
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
      const topicName = get(topicKeys, '[0].name', '');

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
    const { refreshGraph, history, pipeline } = this.props;
    const { name: pipelineName, flows } = pipeline;

    if (this.state.state) {
      toastr.error(
        `The connector is running! Please stop the connector first before deleting`,
      );

      return;
    }

    const connectorResponse = await connectorApi.deleteConnector(
      this.connectorName,
    );

    const connectorHasDeleted = get(connectorResponse, 'data.isSuccess', false);

    const updatedFlows = flows.filter(
      flow => flow.from.name !== this.connectorName,
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

    const isSuccess = get(res, 'data.isSuccess', false);
    this.handleTriggerConnectorResponse(action, isSuccess);
  };

  handleTestConfigs = async (event, values) => {
    event.preventDefault();

    const topic = utils.getCurrTopicId({
      originals: this.props.globalTopics,
      target: values.topicKeys,
    });

    const topicKeys = Array.isArray(topic)
      ? topic
      : [{ group: 'default', name: topic }];

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

  handleTriggerConnectorResponse = async (action, isSuccess) => {
    if (!isSuccess) return;

    const response = await connectorApi.fetchConnector(this.connectorName);
    const state = get(response, 'data.result.state', null);
    const { graph, updateGraph } = this.props;

    this.setState({ state });
    const currSink = findByGraphName(graph, this.connectorName);
    const update = { ...currSink, state };
    updateGraph({ update, dispatcher: { name: 'CONNECTOR' } });

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

    const topicKeys = Array.isArray(topic)
      ? topic
      : [{ group: 'default', name: topic }];

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

    updateGraph({
      update,
      dispatcher: { name: 'CONNECTOR' },
      ...sinkProps,
    });
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
                <form>
                  <AutoSave
                    save={this.handleSave}
                    updateHasChanges={updateHasChanges}
                  />

                  {utils.renderForm({ parentValues: values, ...formProps })}
                  <TestConfigBtn
                    handleClick={e => this.handleTestConfigs(e, values)}
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
