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
import { Redirect } from 'react-router-dom';
import { get, isEmpty, debounce } from 'lodash';

import * as URLS from 'constants/urls';
import * as MESSAGES from 'constants/messages';
import * as connectorApi from 'api/connectorApi';
import Controller from './Controller';
import { Box } from 'common/Layout';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { updateTopic, findByGraphId } from '../pipelineUtils/commonUtils';
import { JdbcQuicklyFillIn } from './QuicklyFillIn';
import {
  CONNECTOR_TYPES,
  CONNECTOR_STATES,
  CONNECTOR_ACTIONS,
} from 'constants/pipelines';
import { graphPropType } from 'propTypes/pipeline';

import * as s from './styles';

const Fieldset = styled.fieldset`
  border: none;
  position: relative;
  padding: 0;

  &:after {
    content: '';
    background-color: red;
    width: 100%;
    height: 100%;
    display: ${props => (props.disabled ? 'block' : 'none')};
    position: absolute;
    top: 0;
    left: 0;
    background: rgba(255, 255, 255, 0.5);
    cursor: not-allowed;
  }
`;

class JdbcSource extends React.Component {
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
  };

  selectMaps = {
    writeTopics: 'currWriteTopic',
  };
  dbSchemasHeader = ['Column name', 'Column type'];

  state = {
    name: '',
    state: '',
    table: '',
    writeTopics: [],
    username: '',
    password: '',
    url: '',
    timestamp: '',
    isBtnWorking: false,
    isFormDisabled: false,
    isRedirect: false,
  };

  componentDidMount() {
    this.fetchSource();
  }

  async componentDidUpdate(prevProps) {
    const { pipelineTopics: prevTopics } = prevProps;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { hasChanges, pipelineTopics: currTopics } = this.props;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevTopics !== currTopics) {
      this.setState({ writeTopics: currTopics });
    }

    if (prevConnectorId !== currConnectorId) {
      this.fetchSource();
    }

    if (hasChanges) {
      this.save();
    }
  }

  fetchSource = async () => {
    const sourceId = get(this.props.match, 'params.connectorId', null);
    const res = await connectorApi.fetchConnector(sourceId);
    const result = get(res, 'data.result', null);

    if (result) {
      const { name, state, topics: prevTopics, configs } = result;
      const {
        'source.timestamp.column.name': timestamp = '',
        'source.db.username': username = '',
        'source.db.password': password = '',
        'source.db.url': url = '',
        table = '',
      } = configs;

      const { pipelineTopics: writeTopics } = this.props;

      if (!isEmpty(prevTopics)) {
        const currWriteTopic = writeTopics.find(
          topic => topic.id === prevTopics[0],
        );

        updateTopic(this.props, currWriteTopic, 'source');
        this.setState({ currWriteTopic });
      }

      const hasValidProps = [username, password, url].map(x => {
        return x.length > 0;
      });

      const isFormDisabled = !hasValidProps.every(x => x === true);

      this.setState({
        name,
        state,
        isFormDisabled,
        table,
        timestamp,
        password,
        username,
        url,
        writeTopics,
      });
    }
  };

  handleInputChange = ({ target: { name, value } }) => {
    this.setState({ [name]: value }, () => {
      this.props.updateHasChanges(true);
    });
  };

  handleSelectChange = ({ target }) => {
    const { name, options, value } = target;
    const selectedIdx = options.selectedIndex;
    const { id } = options[selectedIdx].dataset;

    const current = this.selectMaps[name];

    this.setState(
      () => {
        return {
          [current]: value
            ? {
                name: value,
                id,
              }
            : {},
        };
      },
      () => {
        this.props.updateHasChanges(true);
      },
    );
  };

  updateIsBtnWorking = update => {
    this.setState({ isBtnWorking: update });
  };

  save = debounce(async () => {
    const {
      match,
      updateHasChanges,
      isPipelineRunning,
      graph,
      updateGraph,
    } = this.props;
    const {
      name,
      currWriteTopic,
      table,
      timestamp,
      username,
      password,
      url,
    } = this.state;

    if (isPipelineRunning) {
      toastr.error(MESSAGES.CANNOT_UPDATE_WHILE_RUNNING_ERROR);
      updateHasChanges(false);
      return;
    }

    const sourceId = get(match, 'params.connectorId', null);
    const topics = isEmpty(currWriteTopic) ? [] : [currWriteTopic.id];

    const params = {
      name,
      'connector.name': name,
      schema: [],
      className: CONNECTOR_TYPES.jdbcSource,
      topics,
      numberOfTasks: 1,
      configs: {
        'source.table.name': table,
        'source.db.url': url,
        'source.db.username': username,
        'source.db.password': password,
        'source.timestamp.column.name': timestamp,
        table,
      },
    };

    await connectorApi.updateConnector({ id: sourceId, params });
    updateHasChanges(false);

    const currSource = findByGraphId(graph, sourceId);
    const topicId = isEmpty(topics) ? [] : topics;
    const update = { ...currSource, name, to: topicId };
    updateGraph({ update });
  }, 1000);

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
    const { match, graph, updateGraph } = this.props;
    const sourceId = get(match, 'params.connectorId', null);
    let res;
    if (action === CONNECTOR_ACTIONS.start) {
      res = await connectorApi.startConnector(sourceId);
    } else {
      res = await connectorApi.stopConnector(sourceId);
    }
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      const state = get(res, 'data.result.state');
      this.setState({ state });
      const currSource = findByGraphId(graph, sourceId);
      const update = { ...currSource, state };
      updateGraph({ update });
    }
  };

  quicklyFillIn = values => {
    this.setState(
      {
        url: values.url,
        username: values.user,
        password: values.password,
      },
      () => {
        this.props.updateHasChanges(true);
      },
    );
  };

  render() {
    const {
      name,
      state,
      url,
      username,
      password,
      isBtnWorking,
      table,
      timestamp,
      writeTopics,
      currWriteTopic,
      isRedirect,
    } = this.state;

    if (isRedirect) {
      return <Redirect to={URLS.PIPELINE} />;
    }

    const isRunning = state === CONNECTOR_STATES.running;

    return (
      <React.Fragment>
        <Box>
          <s.TitleWrapper>
            <s.H5Wrapper>JDBC source connector</s.H5Wrapper>
            <Controller
              kind="connector"
              onStart={this.handleStartConnector}
              onStop={this.handleStopConnector}
              onDelete={this.handleDeleteConnector}
            />
          </s.TitleWrapper>
          <Fieldset disabled={isBtnWorking}>
            <FormGroup data-testid="name">
              <Label>Name</Label>
              <Input
                name="name"
                width="100%"
                placeholder="JDBC source name"
                value={name}
                data-testid="name-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
            </FormGroup>

            <FormGroup>
              <Label>URL</Label>
              <Input
                name="url"
                width="100%"
                placeholder="jdbc:postgresql://localhost:5432/db"
                value={url}
                data-testid="url-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
              {/* Incomplete feature, don't display this for now */}
              {false && <JdbcQuicklyFillIn onFillIn={this.quicklyFillIn} />}
            </FormGroup>

            <FormGroup>
              <Label>User name</Label>
              <Input
                name="username"
                width="100%"
                placeholder="admin"
                value={username}
                data-testid="username-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
            </FormGroup>

            <FormGroup>
              <Label>Password</Label>
              <Input
                type="password"
                name="password"
                width="100%"
                placeholder="password"
                value={password}
                data-testid="password-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
            </FormGroup>
          </Fieldset>
          <Fieldset disabled={isBtnWorking}>
            <FormGroup>
              <Label>Table</Label>
              <Input
                name="table"
                width="100%"
                placeholder="tableName"
                value={table}
                data-testid="table-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
            </FormGroup>

            <FormGroup>
              <Label>Timestamp column</Label>
              <Input
                name="timestamp"
                width="100%"
                placeholder="column1"
                value={timestamp}
                data-testid="timestamp-input"
                handleChange={this.handleInputChange}
                disabled={isRunning}
              />
            </FormGroup>

            <FormGroup>
              <Label>Write topic</Label>
              <Select
                isObject
                name="writeTopics"
                list={writeTopics}
                selected={currWriteTopic}
                width="100%"
                data-testid="write-topic-select"
                handleChange={this.handleSelectChange}
                disabled={isRunning}
                placeholder="Please select a topic..."
                clearable
              />
            </FormGroup>
          </Fieldset>
        </Box>
      </React.Fragment>
    );
  }
}

export default JdbcSource;
