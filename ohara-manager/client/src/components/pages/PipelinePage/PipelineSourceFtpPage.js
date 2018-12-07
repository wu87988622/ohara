import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import { H5 } from 'common/Headings';
import { SchemaTable } from 'common/Table';
import { ConfirmModal, Modal } from 'common/Modal';
import { primaryBtn } from 'theme/btnTheme';
import { lightBlue } from 'theme/variables';
import { Input, Select, FormGroup, Label, Button } from 'common/Form';
import { Tab, Tabs, TabList, TabPanel } from 'common/Tabs';
import { fetchTopics } from 'apis/topicApis';
import {
  checkSource,
  createSource,
  updateSource,
  fetchSource,
  fetchPipeline,
  updatePipeline,
} from 'apis/pipelinesApis';

const H5Wrapper = styled(H5)`
  margin: 0;
  font-weight: normal;
  color: ${lightBlue};
`;
H5Wrapper.displayName = 'H5';

const FormGroupWrapper = styled.div`
  display: flex;
  justify-content: space-between;
`;

const NewRowBtn = styled(Button)`
  margin-left: auto;
`;
NewRowBtn.displayName = 'NewRowBtn';

const FormInner = styled.div`
  padding: 20px;
`;

class PipelineSourceFtpPage extends React.Component {
  static propTypes = {
    hasChanges: PropTypes.bool.isRequired,
    updateHasChanges: PropTypes.func.isRequired,
    updateGraph: PropTypes.func.isRequired,
    loadGraph: PropTypes.func.isRequired,
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
  };

  selectMaps = {
    tasks: 'currTask',
    writeTopics: 'currWriteTopic',
    fileEncodings: 'currFileEncoding',
    types: 'currType',
  };

  schemaHeader = [
    '#',
    'Column name',
    'New column name',
    'Type',
    'Up',
    'Down',
    'Delete',
  ];

  schemaTypes = ['string', 'integer', 'boolean'];

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
    tasks: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'],
    currTask: {},
    schema: [],
    isDeleteRowModalActive: false,
    isNewRowModalActive: false,
    workingRow: null,
    columnName: '',
    newColumnName: '',
    currType: '',
    pipelines: {},
  };

  componentDidMount() {
    this.fetchData();
  }

  componentDidUpdate(prevProps) {
    const { hasChanges, match } = this.props;
    const prevSourceId = _.get(prevProps.match, 'params.sourceId', null);
    const currSourceId = _.get(match, 'params.sourceId', null);
    const topicId = _.get(match, 'params.topicId');
    const isUpdate = prevSourceId !== currSourceId;

    if (hasChanges) {
      this.save();
    }

    if (isUpdate) {
      const { name, uuid, rules } = this.state.pipelines;

      const params = {
        name,
        rules: { ...rules, [currSourceId]: topicId },
      };

      this.updatePipeline(uuid, params);
    }
  }

  setDefaults = () => {
    this.setState(({ fileEncodings, tasks }) => ({
      currFileEncoding: fileEncodings[0],
      currTask: tasks[0],
    }));
  };

  fetchData = () => {
    const { match } = this.props;
    const topicId = _.get(match, 'params.topicId', null);
    const sourceId = _.get(match, 'params.sourceId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);

    this.setDefaults();

    if (sourceId) {
      const fetchTopicsPromise = this.fetchTopics(topicId);
      const fetchPipelinePromise = this.fetchPipeline(pipelineId);

      Promise.all([fetchTopicsPromise, fetchPipelinePromise]).then(() => {
        this.fetchSource(sourceId);
      });

      return;
    }

    this.fetchTopics(topicId);
    this.fetchPipeline(pipelineId);
  };

  fetchSource = async sourceId => {
    if (!_.isUuid(sourceId)) return;

    const res = await fetchSource(sourceId);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) return;

    const { schema, name, configs } = res.data.result;
    const {
      'ftp.user.name': username,
      'ftp.user.password': password,
      'ftp.port': port,
      'ftp.hostname': host,
      'ftp.input.folder': inputFolder,
      'ftp.completed.folder': completeFolder,
      'ftp.error.folder': errorFolder,
      'ftp.encode': currFileEncoding,
      currTask,
    } = configs;

    this.setState({
      name,
      host,
      port,
      username,
      password,
      inputFolder,
      completeFolder,
      errorFolder,
      currFileEncoding,
      currTask,
      schema,
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

  fetchPipeline = async pipelineId => {
    if (!_.isUuid(pipelineId)) return;

    const res = await fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', []);

    if (!_.isEmpty(pipelines)) {
      this.setState({ pipelines });

      const sourceId = _.get(this.props.match, 'params.sourceId', null);

      if (sourceId && sourceId !== '__') {
        this.props.loadGraph(pipelines);
      }
    }
  };

  updatePipeline = async (uuid, params) => {
    const res = await updatePipeline({ uuid, params });
    const pipelines = _.get(res, 'data.result', []);

    if (!_.isEmpty(pipelines)) {
      this.setState({ pipelines });
      this.props.loadGraph(pipelines);
    }
  };

  handleInputChange = ({ target: { name, value } }) => {
    this.setState({ [name]: value }, () => {
      const targets = ['columnName', 'newColumnName'];

      if (!targets.includes(name)) {
        this.props.updateHasChanges(true);
      }
    });
  };

  handleSelectChange = ({ target }) => {
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
        if (current !== 'currType') {
          this.props.updateHasChanges(true);
        }
      },
    );
  };

  handleDeleteRowModalOpen = (e, order) => {
    e.preventDefault();
    this.setState({ isDeleteRowModalActive: true, workingRow: order });
  };

  handleDeleteRowModalClose = () => {
    this.setState({ isDeleteRowModalActive: false, workingRow: null });
  };

  handleTypeChange = (e, order) => {
    // https://reactjs.org/docs/events.html#event-pooling
    e.persist();
    this.setState(({ schema }) => {
      const idx = schema.findIndex(schema => schema.order === order);
      const { value: dataType } = e.target;

      const update = {
        ...schema[idx],
        dataType,
      };

      const _schema = [
        ...schema.slice(0, idx),
        update,
        ...schema.slice(idx + 1),
      ];

      return {
        schema: _schema,
      };
    });

    this.props.updateHasChanges(true);
  };

  handleRowDelete = () => {
    if (_.isNull(this.state.workingRow)) return;

    this.setState(({ schema, workingRow }) => {
      const update = schema
        .filter(schema => schema.order !== workingRow)
        .map((schema, idx) => ({ ...schema, order: ++idx }));

      return {
        schema: update,
        isDeleteRowModalActive: false,
      };
    });

    this.props.updateHasChanges(true);
  };

  handleNewRowModalOpen = () => {
    this.setState({
      isNewRowModalActive: true,
      currType: this.schemaTypes[0],
    });
  };

  handleNewRowModalClose = () => {
    this.setState({
      isNewRowModalActive: false,
      currType: '',
      columnName: '',
      newColumnName: '',
    });
  };

  handleRowCreate = () => {
    this.setState(
      ({
        schema,
        columnName: name,
        newColumnName: newName,
        currType: type,
      }) => {
        const _order = _.isEmpty(schema)
          ? 1
          : schema[schema.length - 1].order + 1;

        const newSchema = {
          name,
          newName,
          dataType: type,
          order: _order,
        };

        return {
          isNewRowModalActive: false,
          schema: [...schema, newSchema],
          columnName: '',
          newColumnName: '',
          currType: this.schemaTypes[0],
        };
      },
    );

    this.props.updateHasChanges(true);
  };

  handleUp = (e, order) => {
    e.preventDefault();

    if (order === 1) return;

    this.setState(({ schema }) => {
      const idx = schema.findIndex(s => s.order === order);

      const _schema = [
        ...schema.slice(0, idx - 1),
        schema[idx],
        schema[idx - 1],
        ...schema.slice(idx + 1),
      ].map((schema, idx) => ({ ...schema, order: ++idx }));

      return {
        schema: _schema,
      };
    });

    this.props.updateHasChanges(true);
  };

  handleDown = (e, order) => {
    e.preventDefault();

    if (order === this.state.schema.length) return;

    this.setState(({ schema }) => {
      const idx = schema.findIndex(s => s.order === order);

      const _schema = [
        ...schema.slice(0, idx),
        schema[idx + 1],
        schema[idx],
        ...schema.slice(idx + 2),
      ].map((schema, idx) => ({ ...schema, order: ++idx }));

      return {
        schema: _schema,
      };
    });

    this.props.updateHasChanges(true);
  };

  handleTestConnection = async e => {
    e.preventDefault();
    const { host: hostname, port, username: user, password } = this.state;

    this.updateIsTestConnectionBtnWorking(true);
    const res = await checkSource({ hostname, port, user, password });
    this.updateIsTestConnectionBtnWorking(false);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDisabled: false });
    }
  };

  updateIsTestConnectionBtnWorking = update => {
    this.setState({ IsTestConnectionBtnWorking: update });
  };

  save = _.debounce(async () => {
    const { match, history, updateHasChanges, isPipelineRunning } = this.props;
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
      schema,
    } = this.state;

    if (isPipelineRunning) {
      toastr.error(MESSAGES.CANNOT_UPDATE_WHILE_RUNNING_ERROR);
      updateHasChanges(false);
      return;
    }

    const sourceId = _.get(match, 'params.sourceId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);
    const sourceIdPlaceHolder = '__';
    const isCreate =
      _.isNull(sourceId) || sourceId === sourceIdPlaceHolder ? true : false;
    const _schema = _.isEmpty(schema) ? [] : schema;

    const params = {
      name,
      schema: _schema,
      className: 'ftp',
      topics: [currWriteTopic.uuid],
      numberOfTasks: 1,
      configs: {
        'ftp.input.folder': inputFolder,
        'ftp.completed.folder': completeFolder,
        'ftp.error.folder': errorFolder,
        'ftp.encode': currFileEncoding,
        'ftp.hostname': host,
        'ftp.port': port,
        'ftp.user.name': username,
        'ftp.user.password': password,

        topic: currWriteTopic.name,
        currTask,
      },
    };

    const res = isCreate
      ? await createSource(params)
      : await updateSource({ uuid: sourceId, params });

    const _sourceId = _.get(res, 'data.result.uuid', null);
    await this.fetchPipeline(pipelineId);

    if (_sourceId) {
      updateHasChanges(false);
      if (isCreate && !sourceId) {
        history.push(`${match.url}/${_sourceId}`);
      } else if (isCreate && sourceId) {
        const paths = match.url.split(sourceIdPlaceHolder);
        history.push(`${paths[0]}${_sourceId}${paths[1]}`);
      }
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
      IsTestConnectionBtnWorking,
      currTask,
      schema,
      isDeleteRowModalActive,
      isNewRowModalActive,
      columnName,
      newColumnName,
      currType,
    } = this.state;

    return (
      <React.Fragment>
        <Tabs>
          <TabList>
            <Tab>FTP Source 1/2</Tab>
            <Tab>FTP Source 2/2</Tab>
            <Tab>Output schema</Tab>
          </TabList>
          <ConfirmModal
            isActive={isDeleteRowModalActive}
            title="Delete row?"
            confirmBtnText="Yes, Delete this row"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeleteRowModalClose}
            handleConfirm={this.handleRowDelete}
            message="Are you sure you want to delete this row? This action cannot be redo!"
            isDelete
          />

          <Modal
            isActive={isNewRowModalActive}
            title="New row"
            width="290px"
            confirmBtnText="Create"
            handleConfirm={this.handleRowCreate}
            handleCancel={this.handleNewRowModalClose}
          >
            <form>
              <FormInner>
                <FormGroup>
                  <Label>Column name</Label>
                  <Input
                    name="columnName"
                    width="250px"
                    placeholder="Column name"
                    value={columnName}
                    data-testid="column-name-modal"
                    handleChange={this.handleInputChange}
                  />
                </FormGroup>

                <FormGroup>
                  <Label>New column name</Label>
                  <Input
                    name="newColumnName"
                    width="100%"
                    placeholder="New column name"
                    value={newColumnName}
                    data-testid="new-column-name-modal"
                    handleChange={this.handleInputChange}
                  />
                </FormGroup>

                <FormGroup>
                  <Label>Type</Label>
                  <Select
                    name="types"
                    width="100%"
                    list={this.schemaTypes}
                    selected={currType}
                    handleChange={this.handleSelectChange}
                  />
                </FormGroup>
              </FormInner>
            </form>
          </Modal>
          <TabPanel>
            <form>
              <FormGroup>
                <Label>Name</Label>
                <Input
                  name="name"
                  width="100%"
                  placeholder="FTP source name"
                  value={name}
                  data-testid="name-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>FTP host</Label>
                <Input
                  name="host"
                  width="100%"
                  placeholder="http://localhost"
                  value={host}
                  data-testid="host-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>FTP port</Label>
                <Input
                  name="port"
                  width="100%"
                  placeholder="21"
                  value={port}
                  data-testid="port-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>User name</Label>
                <Input
                  name="username"
                  width="100%"
                  placeholder="John Doe"
                  value={username}
                  data-testid="username-input"
                  handleChange={this.handleInputChange}
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
                />
              </FormGroup>

              <FormGroup>
                <Button
                  theme={primaryBtn}
                  text="Test Connection"
                  isWorking={IsTestConnectionBtnWorking}
                  disabled={IsTestConnectionBtnWorking}
                  data-testid="test-connection-btn"
                  handleClick={this.handleTestConnection}
                />
              </FormGroup>
            </form>
          </TabPanel>

          <TabPanel>
            <form>
              <FormGroupWrapper>
                <FormGroup css={{ width: '70%', margin: '0 20px 0 0' }}>
                  <Label>File encoding</Label>
                  <Select
                    name="fileEnconding"
                    list={fileEncodings}
                    selected={currFileEncoding}
                    data-testid="file-enconding-select"
                    handleChange={this.handleSelectChange}
                  />
                </FormGroup>

                <FormGroup width="30%">
                  <Label>Task</Label>
                  <Select
                    name="tasks"
                    list={tasks}
                    selected={currTask}
                    data-testid="task-select"
                    handleChange={this.handleSelectChange}
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
                  width="100%"
                  data-testid="write-topic-select"
                  handleChange={this.handleSelectChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>Input folder</Label>
                <Input
                  name="inputFolder"
                  width="100%"
                  placeholder="/path/to/the/input/folder"
                  value={inputFolder}
                  data-testid="input-folder-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>Complete folder</Label>
                <Input
                  name="completeFolder"
                  width="100%"
                  placeholder="/path/to/the/complete/folder"
                  value={completeFolder}
                  data-testid="complete-folder-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>Error folder</Label>
                <Input
                  name="errorFolder"
                  width="100%"
                  placeholder="/path/to/the/error/folder"
                  value={errorFolder}
                  data-testid="error-folder-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>
            </form>
          </TabPanel>
          <TabPanel>
            <NewRowBtn
              text="New row"
              theme={primaryBtn}
              data-testid="new-row-btn"
              handleClick={this.handleNewRowModalOpen}
            />
            <SchemaTable
              headers={this.schemaHeader}
              schema={schema}
              dataTypes={this.schemaTypes}
              handleTypeChange={this.handleTypeChange}
              handleModalOpen={this.handleDeleteRowModalOpen}
              handleUp={this.handleUp}
              handleDown={this.handleDown}
            />
          </TabPanel>
        </Tabs>
      </React.Fragment>
    );
  }
}

export default PipelineSourceFtpPage;
