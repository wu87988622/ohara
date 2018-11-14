import React from 'react';
import styled from 'styled-components';
import toastr from 'toastr';
import PropTypes from 'prop-types';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import { Box } from 'common/Layout';
import { H5 } from 'common/Headings';
import { SchemaTable } from 'common/Table';
import { ConfirmModal, Modal } from 'common/Modal';
import { primaryBtn } from 'theme/btnTheme';
import { lightBlue, whiteSmoke } from 'theme/variables';
import { Input, Select, FormGroup, Label, Button } from 'common/Form';
import { fetchTopics } from 'apis/topicApis';
import {
  createSink,
  updateSink,
  fetchSink,
  updatePipeline,
  fetchPipeline,
  validateFtp,
} from 'apis/pipelinesApis';

const H5Wrapper = styled(H5)`
  margin: 0;
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

const FormGroupCheckbox = styled(FormGroup)`
  flex-direction: row;
  align-items: center;
  color: ${lightBlue};
`;

const Checkbox = styled(Input)`
  height: auto;
  width: auto;
  margin-right: 8px;
`;

const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: 25px;
`;

const SchemaBtn = styled(Button)`
  margin-left: auto;
`;

const FormInner = styled.div`
  padding: 20px;
`;

class PipelineSinkFtpPage extends React.Component {
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
    fileEncodings: 'currFileEncoding',
    readTopics: 'currReadTopic',
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
    outputfolder: '',
    readTopics: [],
    schema: [],
    currReadTopic: {},
    fileEncodings: ['UTF-8'],
    currFileEncoding: {},
    tasks: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'],
    currTask: {},
    needHeader: true,
    isNewSchemaModalActive: false,
    isDeleteSchemaModalActive: false,
    columnName: '',
    newColumnName: '',
    IsTestConnectionBtnWorking: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentDidUpdate(prevProps) {
    const { hasChanges, match } = this.props;

    const prevSinkId = _.get(prevProps.match, 'params.sinkId', null);
    const currSinkId = _.get(this.props.match, 'params.sinkId', null);
    const topicId = _.get(match, 'params.topicId');
    const hasTopicId = !_.isNull(topicId);
    const isUpdate = prevSinkId !== currSinkId;

    if (hasChanges) {
      this.save();
    }

    if (isUpdate && hasTopicId) {
      const { name, uuid, rules } = this.state.pipelines;

      const params = {
        name,
        rules: { ...rules, [topicId]: currSinkId },
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
    const sinkId = _.get(match, 'params.sinkId', null);
    const topicId = _.get(match, 'params.topicId', null);
    const pipelineId = _.get(match, 'params.pipelineId', null);

    this.setDefaults();

    if (sinkId) {
      const fetchTopicsPromise = this.fetchTopics(topicId);
      const fetchPipelinePromise = this.fetchPipeline(pipelineId);

      Promise.all([fetchPipelinePromise, fetchTopicsPromise]).then(() => {
        this.fetchSink(sinkId);
      });

      return;
    }

    this.fetchTopics(topicId);
    this.fetchPipeline(pipelineId);
  };

  fetchTopics = async topicId => {
    if (!_.isUuid(topicId)) return;

    const res = await fetchTopics();
    const readTopics = _.get(res, 'data.result', []);
    if (!_.isEmpty(readTopics)) {
      const { name, uuid } = readTopics.find(({ uuid }) => uuid === topicId);
      this.setState({ readTopics, currReadTopic: { name, uuid } });
    } else {
      toastr.error(MESSAGES.INVALID_TOPIC_ID);
      this.setState({ isRedirect: true });
    }
  };

  fetchSink = async sourceId => {
    if (!_.isUuid(sourceId)) return;

    const res = await fetchSink(sourceId);
    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (!isSuccess) return;

    const { schema, name, configs } = res.data.result;
    const {
      'ftp.hostname': host,
      'ftp.port': port,
      'ftp.user.name': username,
      'ftp.user.password': password,
      'ftp.output.folder': outputfolder,
      'ftp.encode': currFileEncoding,
      'ftp.needHeader': needHeader,
      currTask,
    } = configs;

    const _needHeader = needHeader === 'true' ? true : false;

    this.setState({
      name,
      host,
      port,
      username,
      password,
      outputfolder,
      currFileEncoding,
      currTask,
      needHeader: _needHeader,
      schema,
    });
  };

  fetchPipeline = async pipelineId => {
    if (!_.isUuid(pipelineId)) return;

    const res = await fetchPipeline(pipelineId);
    const pipelines = _.get(res, 'data.result', null);

    if (pipelines) {
      this.setState({ pipelines });

      const sinkId = _.get(this.props.match, 'params.sinkId', null);

      if (sinkId) {
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
      this.props.updateHasChanges(true);
    });
  };

  handleCheckboxChange = ({ target }) => {
    const { name, checked } = target;
    this.setState({ [name]: checked }, () => {
      this.props.updateHasChanges(true);
    });
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

  handleDeleteSchemaModalOpen = (e, order) => {
    e.preventDefault();
    this.setState({ isDeleteSchemaModalActive: true, workingRow: order });
  };

  handleDeleteSchemaModalClose = () => {
    this.setState({ isDeleteSchemaModalActive: false, workingRow: null });
  };

  handleSchemaDelete = () => {
    if (_.isNull(this.state.workingRow)) return;

    this.setState(({ schema, workingRow }) => {
      const update = schema
        .filter(schema => schema.order !== workingRow)
        .map((schema, idx) => ({ ...schema, order: ++idx }));

      return {
        schema: update,
        isDeleteSchemaModalActive: false,
      };
    });

    this.props.updateHasChanges(true);
  };

  handleNewSchemaModalOpen = () => {
    this.setState({
      isNewSchemaModalActive: true,
      currType: this.schemaTypes[0],
    });
  };

  handleNewSchemaModalClose = () => {
    this.setState({
      isNewSchemaModalActive: false,
      currType: '',
      columnName: '',
      newColumnName: '',
    });
  };

  handleSchemaCreate = () => {
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
          isNewSchemaModalActive: false,
          schema: [...schema, newSchema],
          columnName: '',
          newColumnName: '',
          currType: this.schemaTypes[0],
        };
      },
    );
    this.props.updateHasChanges(true);
  };

  handleTestConnection = async e => {
    e.preventDefault();
    const { host: hostname, port, username: user, password } = this.state;

    this.updateIsTestConnectionBtnWorking(true);
    const res = await validateFtp({
      hostname,
      port,
      user,
      password,
    });
    this.updateIsTestConnectionBtnWorking(false);

    const _res = _.get(res, 'data.isSuccess', false);

    if (_res) {
      toastr.success(MESSAGES.TEST_SUCCESS);
    }
  };

  updateIsTestConnectionBtnWorking = update => {
    this.setState({ IsTestConnectionBtnWorking: update });
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

  save = _.debounce(async () => {
    const { match, history, updateHasChanges } = this.props;
    const {
      name,
      host,
      port,
      username,
      password,
      outputfolder,
      needHeader,
      currReadTopic,
      currFileEncoding,
      currTask,
      schema,
    } = this.state;

    const sourceId = _.get(match, 'params.sourceId', null);
    const sinkId = _.get(match, 'params.sinkId', null);
    const isCreate = _.isNull(sinkId) ? true : false;
    const hasSourceId = _.isNull(sourceId) ? false : true;
    const _schema = _.isEmpty(schema) ? [] : schema;

    const params = {
      name,
      schema: _schema,
      className: 'ftp',
      topics: [currReadTopic.uuid],
      numberOfTasks: 1,
      configs: {
        'ftp.output.folder': outputfolder,
        'ftp.encode': currFileEncoding,
        'ftp.hostname': host,
        'ftp.port': port,
        'ftp.user.name': username,
        'ftp.user.password': password,
        'ftp.needHeader': String(needHeader),
        currTask,
        topic: currReadTopic.name,
      },
    };

    const res = isCreate
      ? await createSink(params)
      : await updateSink({ uuid: sinkId, params });

    const _sinkId = _.get(res, 'data.result.uuid', null);

    if (_sinkId) {
      updateHasChanges(false);

      if (isCreate && !hasSourceId) history.push(`${match.url}/__/${_sinkId}`);
      if (isCreate && hasSourceId) history.push(`${match.url}/${_sinkId}`);
    }
  }, 1000);

  render() {
    const {
      name,
      host,
      port,
      username,
      password,
      fileEncodings,
      currFileEncoding,
      tasks,
      schema,
      currTask,
      isNewSchemaModalActive,
      readTopics,
      currReadTopic,
      outputfolder,
      needHeader,
      columnName,
      newColumnName,
      currType,
      isDeleteSchemaModalActive,
      IsTestConnectionBtnWorking,
    } = this.state;

    return (
      <React.Fragment>
        <Box>
          <ConfirmModal
            isActive={isDeleteSchemaModalActive}
            title="Delete schema?"
            confirmBtnText="Yes, Delete this schema"
            cancelBtnText="No, Keep it"
            handleCancel={this.handleDeleteSchemaModalClose}
            handleConfirm={this.handleSchemaDelete}
            message="Are you sure you want to delete this schema? This action cannot be redo!"
            isDelete
          />

          <Modal
            isActive={isNewSchemaModalActive}
            title="New schema"
            width="290px"
            confirmBtnText="Create"
            handleConfirm={this.handleSchemaCreate}
            handleCancel={this.handleNewSchemaModalClose}
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
                    width="250px"
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
                    width="250px"
                    list={this.schemaTypes}
                    selected={currType}
                    handleChange={this.handleSelectChange}
                  />
                </FormGroup>
              </FormInner>
            </form>
          </Modal>
          <H5Wrapper>FTP Sink</H5Wrapper>
          <Form>
            <LeftCol>
              <FormGroup>
                <Label>Name</Label>
                <Input
                  name="name"
                  width="250px"
                  placeholder="FTP sink name"
                  value={name}
                  data-testid="name-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>FTP host</Label>
                <Input
                  name="host"
                  width="250px"
                  placeholder="ftp://localhost"
                  value={host}
                  data-testid="host-input"
                  handleChange={this.handleInputChange}
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
                  handleChange={this.handleInputChange}
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
                  handleChange={this.handleInputChange}
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
                  handleChange={this.handleInputChange}
                />
              </FormGroup>
              <FormGroup>
                <Button
                  theme={primaryBtn}
                  text="Test connection"
                  isWorking={IsTestConnectionBtnWorking}
                  disabled={IsTestConnectionBtnWorking}
                  data-testid="test-connection-btn"
                  handleClick={this.handleTestConnection}
                />
              </FormGroup>
            </LeftCol>

            <RightCol>
              <FormGroupWrapper>
                <FormGroup>
                  <Label>File encoding</Label>
                  <Select
                    name="fileEnconding"
                    width="140px"
                    data-testid="file-enconding-select"
                    selected={currFileEncoding}
                    list={fileEncodings}
                    handleChange={this.handleSelectChange}
                  />
                </FormGroup>

                <FormGroup>
                  <Label>Task</Label>
                  <Select
                    name="tasks"
                    width="85px"
                    data-testid="task-select"
                    selected={currTask}
                    list={tasks}
                    handleChange={this.handleSelectChange}
                  />
                </FormGroup>
              </FormGroupWrapper>

              <FormGroup>
                <Label>Read topic</Label>
                <Select
                  isObject
                  name="readTopics"
                  width="250px"
                  data-testid="read-topic-select"
                  selected={currReadTopic}
                  list={readTopics}
                  handleChange={this.handleSelectChange}
                />
              </FormGroup>

              <FormGroup>
                <Label>Output Folder</Label>
                <Input
                  name="outputfolder"
                  width="250px"
                  placeholder="/home/user1"
                  value={outputfolder}
                  data-testid="outputfolder-input"
                  handleChange={this.handleInputChange}
                />
              </FormGroup>

              <FormGroupCheckbox>
                <Checkbox
                  type="checkbox"
                  name="needHeader"
                  width="25px"
                  value=""
                  checked={needHeader}
                  data-testid="needheader-input"
                  handleChange={this.handleCheckboxChange}
                />
                Include header
              </FormGroupCheckbox>
            </RightCol>
          </Form>
        </Box>
        <Box>
          <SectionHeader>
            <H5Wrapper>Output schema</H5Wrapper>
            <SchemaBtn
              text="New schema"
              theme={primaryBtn}
              data-testid="new-topic"
              handleClick={this.handleNewSchemaModalOpen}
            />
          </SectionHeader>
          <SchemaTable
            headers={this.schemaHeader}
            schema={schema}
            dataTypes={this.schemaTypes}
            handleTypeChange={this.handleTypeChange}
            handleModalOpen={this.handleDeleteSchemaModalOpen}
            handleUp={this.handleUp}
            handleDown={this.handleDown}
          />
        </Box>
      </React.Fragment>
    );
  }
}

export default PipelineSinkFtpPage;
