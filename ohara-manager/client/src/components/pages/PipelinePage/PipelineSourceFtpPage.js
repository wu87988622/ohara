import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';

import * as _ from 'utils/helpers';
import * as MESSAGES from 'constants/messages';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { lightBlue, whiteSmoke } from 'theme/variables';
import { Input, Select, FormGroup, Label } from 'common/Form';
import { fetchTopics } from 'apis/topicApis';
import { createSource, updateSource, fetchSource } from 'apis/pipelinesApis';

const H5Wrapper = styled(H5)`
  margin: 0 0 30px;
  font-weight: normal;
  color: ${lightBlue};
`;

const Form = styled.form`
  display: flex;
`;

const LeftCol = styled.div`
  width: 250px;
  padding-right: 45px;
  margin-right: 45px;
  border-right: 2px solid ${whiteSmoke};
  box-sizing: content-box;
`;

const RightCol = styled.div`
  width: 250px;
`;

const FormGroupWrapper = styled.div`
  display: flex;
  justify-content: space-between;
`;

class PipelineSourceFtpPage extends React.Component {
  selectMaps = {
    tasks: 'currTask',
    writeTopics: 'currWriteTopic',
    fileEncodings: 'currFileEncoding',
  };

  state = {
    name: '',
    host: '',
    port: '',
    username: '',
    password: '',
    inputFolder: '',
    completeFolder: '',
    errorFolder: '',
    writeTopics: [],
    currWriteTopic: {},
    fileEncodings: ['UTF-8'],
    currFileEncoding: {},
    tasks: ['1-10', '11-20', '21-30'],
    currTask: {},
  };

  componentDidMount() {
    const { match } = this.props;
    const sourceId = _.get(match, 'params.sourceId', null);
    const topicId = _.get(match, 'params.topicId', null);

    this.setDefaults();

    if (sourceId) {
      this.fetchSource(sourceId);
    }

    if (topicId) {
      this.fetchTopics(topicId);
    }
  }

  componentDidUpdate() {
    const { hasChanges } = this.props;

    if (hasChanges) {
      this.save();
    }
  }

  setDefaults = () => {
    this.setState(({ fileEncodings, tasks }) => ({
      currFileEncoding: fileEncodings[0],
      currTask: tasks[0],
    }));
  };

  fetchSource = async sourceId => {
    if (!_.isUuid(sourceId)) return;

    const res = await fetchSource(sourceId);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) return;

    const {
      name,
      host,
      port,
      username,
      password,
      inputFolder,
      completeFolder,
      errorFolder,
      currWriteTopic,
      currFileEncoding,
      currTask,
    } = res.data.result.configs;

    this.setState({
      name,
      host,
      port,
      username,
      password,
      inputFolder,
      completeFolder,
      errorFolder,
      currWriteTopic,
      currFileEncoding,
      currTask,
    });
  };

  fetchTopics = async topicId => {
    if (!_.isUuid(topicId)) return;

    const res = await fetchTopics();
    const writeTopics = _.get(res, 'data.result', []);

    if (!_.isEmpty(writeTopics)) {
      const { name, uuid } = writeTopics.find(({ uuid }) => uuid === topicId);
      this.setState({ writeTopics, currWriteTopic: { name, uuid } });
    } else {
      toastr.error(MESSAGES.INVALID_TOPIC_ID);
      this.setState({ isRedirect: true });
    }
  };

  handleChangeInput = ({ target: { name, value } }) => {
    this.setState({ [name]: value }, () => {
      this.props.updateHasChanges(true);
    });
  };

  handleChangeSelect = ({ target }) => {
    const { name, options, value } = target;
    const selectedIdx = options.selectedIndex;
    const { uuid } = options[selectedIdx].dataset;
    const hasUuid = Boolean(uuid);
    const current = this.selectMaps[name];

    if (hasUuid) {
      this.setState(
        () => {
          return {
            [current]: {
              name: value,
              uuid,
            },
          };
        },
        () => {
          this.props.updateHasChanges(true);
        },
      );

      return;
    }

    this.setState(
      () => {
        return {
          [current]: value,
        };
      },
      () => {
        this.props.updateHasChanges(true);
      },
    );
  };

  save = _.debounce(async () => {
    const { match, history } = this.props;
    const {
      name,
      host,
      port,
      username,
      password,
      inputFolder,
      completeFolder,
      errorFolder,
      currWriteTopic,
      currFileEncoding,
      currTask,
    } = this.state;
    const sourceId = _.get(match, 'params.sourceId', null);
    const isCreate = _.isNull(sourceId) ? true : false;

    const params = {
      name: 'untitled source',
      schema: null,
      class: 'ftp',
      configs: {
        name,
        host,
        port,
        username,
        password,
        inputFolder,
        completeFolder,
        errorFolder,
        topic: currWriteTopic.name,
        currFileEncoding,
        currTask,
      },
    };

    const res = isCreate
      ? await createSource(params)
      : await updateSource({ uuid: sourceId, params });

    const uuid = _.get(res, 'data.result.uuid', null);

    if (uuid) {
      this.props.updateHasChanges(false);
      if (isCreate) history.push(`${match.url}/${uuid}`);
    }
  }, 1000);

  render() {
    const {
      name,
      host,
      port,
      username,
      password,
      inputFolder,
      completeFolder,
      errorFolder,
      writeTopics,
      currWriteTopic,
      fileEncodings,
      currFileEncoding,
      tasks,
      currTask,
    } = this.state;

    return (
      <Box>
        <H5Wrapper>FTP Source</H5Wrapper>

        <Form>
          <LeftCol>
            <FormGroup>
              <Label>Name</Label>
              <Input
                name="name"
                width="250px"
                placeholder="FTP source name"
                value={name}
                data-testid="name-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>FTP host</Label>
              <Input
                name="host"
                width="250px"
                placeholder="http://localhost"
                value={host}
                data-testid="host-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>FTP port</Label>
              <Input
                name="port"
                width="250px"
                placeholder="21"
                value={port}
                data-testid="port-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>User name</Label>
              <Input
                name="username"
                width="250px"
                placeholder="John Doe"
                value={username}
                data-testid="username-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>Password</Label>
              <Input
                type="password"
                name="password"
                width="250px"
                placeholder="password"
                value={password}
                data-testid="password-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>
          </LeftCol>
          <RightCol>
            <FormGroupWrapper>
              <FormGroup>
                <Label>File encoding</Label>
                <Select
                  name="fileEnconding"
                  list={fileEncodings}
                  selected={currFileEncoding}
                  width="140px"
                  data-testid="file-enconding-select"
                  handleChange={this.handleChangeSelect}
                />
              </FormGroup>

              <FormGroup>
                <Label>Task</Label>
                <Select
                  name="tasks"
                  list={tasks}
                  selected={currTask}
                  width="85px"
                  data-testid="task-select"
                  handleChange={this.handleChangeSelect}
                />
              </FormGroup>
            </FormGroupWrapper>

            <FormGroup>
              <Label>Write topic</Label>
              <Select
                isObject
                name="writeTopics"
                list={writeTopics}
                selected={currWriteTopic}
                width="250px"
                data-testid="write-topic-select"
                handleChange={this.handleChangeSelect}
              />
            </FormGroup>

            <FormGroup>
              <Label>Input folder</Label>
              <Input
                name="inputFolder"
                width="250px"
                placeholder="/path/to/the/input/folder"
                value={inputFolder}
                data-testid="input-folder-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>Complete folder</Label>
              <Input
                name="completeFolder"
                width="250px"
                placeholder="/path/to/the/complete/folder"
                value={completeFolder}
                data-testid="complete-folder-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>

            <FormGroup>
              <Label>Error folder</Label>
              <Input
                name="errorFolder"
                width="250px"
                placeholder="/path/to/the/error/folder"
                value={errorFolder}
                data-testid="error-folder-input"
                handleChange={this.handleChangeInput}
              />
            </FormGroup>
          </RightCol>
        </Form>
      </Box>
    );
  }
}

PipelineSourceFtpPage.propTypes = {
  prop: PropTypes.array,
};

export default PipelineSourceFtpPage;
