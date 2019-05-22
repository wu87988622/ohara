import React from 'react';
import * as utils from './customConnectorUtils';
import { validateConnector } from 'api/validateApi';
import { get } from 'lodash';
import toastr from 'toastr';
import * as MESSAGES from 'constants/messages';
import * as s from './styles';
import TestConnectionBtn from './TestConnectionBtn';
import { Form } from 'react-final-form';
import { fetchWorker } from 'api/workerApi';
import * as connectorApi from 'api/connectorApi';

class CustomFinalForm extends React.Component {
  state = {
    defs: [],
    topics: [],
    configs: null,
    state: null,
    isTestConnectionBtnWorking: false,
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

  componentDidMount() {
    this.fetchData();
  }

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

  setTopics = () => {
    const { pipelineTopics } = this.props;
    this.setState({ topics: pipelineTopics.map(t => t.name) });
  };

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
      <Form
        onSubmit={this.handleTestConnection}
        render={({
          handleSubmit,
          form,
          submitting,
          pristine,
          invalid,
          values,
        }) => {
          return (
            <div>
              {utils.renderForm(formProps, form, handleSubmit)}
              <s.StyledForm>
                <TestConnectionBtn
                  handleClick={this.handleTestConnection}
                  isWorking={isTestConnectionBtnWorking}
                />
              </s.StyledForm>
            </div>
          );
        }}
      />
    );
  }
}

export default CustomFinalForm;
