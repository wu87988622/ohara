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
import { get, debounce } from 'lodash';

import * as connectorApi from 'api/connectorApi';
import * as MESSAGES from 'constants/messages';
import * as utils from './customConnectorUtils';
import TestConnectionBtn from './TestConnectionBtn';
import { fetchWorkers } from 'api/workerApi';
import { validateConnector } from 'api/validateApi';
import { BoxWrapper, TitleWrapper, H5Wrapper } from '../styles';
import { StyledForm, LoaderWrap } from './styles';
import { ListLoader } from 'common/Loader';
import { graphPropType } from 'propTypes/pipeline';

class CustomConnector extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.shape({
        page: PropTypes.string.isRequired,
        connectorId: PropTypes.string.isRequired,
      }).isRequired,
    }).isRequired,
    graph: PropTypes.arrayOf(graphPropType).isRequired,
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    topics: PropTypes.shape({
      name: PropTypes.string.isRequired,
    }).isRequired,
  };

  state = {
    isLoading: true,
    defs: [],
    topics: [],
    originalTopics: [],
    configs: null,
    isTestConnectionBtnWorking: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentDidUpdate(prevProps) {
    const { hasChanges } = this.props;
    const { connectorId: prevConnectorId } = prevProps.match.params;
    const { connectorId: currConnectorId } = this.props.match.params;

    if (prevConnectorId !== currConnectorId) {
      this.fetchData();
    }

    if (hasChanges) {
      this.save();
    }
  }

  setTopics = () => {
    const { topics } = this.props;
    this.setState({
      topics: topics.map(topic => topic.name),
      originalTopics: topics,
    });
  };

  fetchData = async () => {
    // We need to get the custom connector's meta data first
    await this.fetchWorker();

    // After the form is rendered, let's fetch connector data and override the default values from meta data
    this.fetchConnector();
    this.setTopics();
  };

  fetchWorker = async () => {
    const res = await fetchWorkers();
    const workers = get(res, 'data.result', null);
    this.setState({ isLoading: false });

    if (workers) {
      const { defs, configs } = utils.getMetadata(this.props, workers);
      this.setState({ defs, configs });
    }
  };

  fetchConnector = async () => {
    const { connectorId } = this.props.match.params;
    const res = await connectorApi.fetchConnector(connectorId);
    const result = get(res, 'data.result', null);

    if (result) {
      const topicName = utils.getCurrTopicName({
        originals: this.state.originalTopics,
        target: result.settings.topics,
      });

      const configs = { ...result.settings, topics: topicName };
      this.setState({ configs });
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
    const params = { ...this.state.configs, topics: [] };
    const res = await validateConnector(params);
    this.setState({ isTestConnectionBtnWorking: false });

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
  };

  save = debounce(async () => {
    const { updateHasChanges, match, graph, updateGraph } = this.props;
    const { configs, originalTopics } = this.state;
    const { connectorId } = match.params;

    const topicId = utils.getCurrTopicId({
      originals: originalTopics,
      target: configs.topics,
    });

    const params = { ...configs, topics: topicId };

    await connectorApi.updateConnector({ id: connectorId, params });
    updateHasChanges(false);

    const update = utils.getUpdatedTopic({
      graph,
      connectorId,
      configs,
      originalTopics,
    });

    updateGraph({ update });
  }, 1000);

  render() {
    const {
      defs,
      configs,
      isLoading,
      topics,
      isTestConnectionBtnWorking,
    } = this.state;

    const formProps = {
      defs,
      configs,
      topics,
      handleChange: this.handleChange,
      handleColumnChange: this.handleColumnChange,
      handleColumnRowDelete: this.handleColumnRowDelete,
      handleColumnRowUp: this.handleColumnRowUp,
      handleColumnRowDown: this.handleColumnRowDown,
    };

    return (
      <BoxWrapper padding="25px 0 0 0">
        <TitleWrapper margin="0 25px 30px">
          <H5Wrapper>Custom connector</H5Wrapper>
        </TitleWrapper>
        {isLoading ? (
          <LoaderWrap>
            <ListLoader />
          </LoaderWrap>
        ) : (
          <StyledForm>
            {utils.renderForm(formProps)}

            {// This feature belongs to another story...
            false && (
              <TestConnectionBtn
                handleClick={this.handleTestConnection}
                isWorking={isTestConnectionBtnWorking}
              />
            )}
          </StyledForm>
        )}
      </BoxWrapper>
    );
  }
}

CustomConnector.propTypes = {
  workerClusterName: PropTypes.string.isRequired,
};

export default CustomConnector;
