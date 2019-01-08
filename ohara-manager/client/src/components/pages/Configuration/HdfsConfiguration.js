import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import toastr from 'toastr';

import * as _ from 'utils/commonUtils';
import * as CSS_VARS from 'theme/variables';
import * as MESSAGES from 'constants/messages';
import * as configurationApis from 'apis/configurationApis';
import { Input, Button, FormGroup } from 'common/Form';
import { cancelBtn, primaryBtn, defaultBtn } from 'theme/btnTheme';

const modalStyles = {
  content: {
    top: '15%',
    left: '50%',
    right: 'auto',
    bottom: 'auto',
    marginRight: '-50%',
    transform: 'translate(-50%, 0)',
    padding: 0,
  },
};

const ModalBody = styled.div`
  display: flex;
`;

const Connections = styled.div`
  padding: 0;
  width: 200px;
  overflow-y: auto;
  background-color: ${CSS_VARS.whiteSmoke};
`;

const HdfsForm = styled.div`
  padding: 20px;
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
  border-top: 1px solid ${CSS_VARS.lighterGray};
  justify-content: flex-end;
`;

const Label = styled.label`
  color: ${CSS_VARS.lightBlue};
  margin-bottom: 20px;
`;

const TestBtn = styled(Button)`
  margin-top: 20px;
  color: ${CSS_VARS.lighterBlue};
  width: 140px;

  /* Prevent button from wrapping if the btn is working and therefore an extra loading icon is displayed */
  white-space: nowrap;
`;

const AddIcon = styled.button`
  color: ${CSS_VARS.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 20px;
  background: transparent;

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const DeleteIcon = styled.button`
  color: ${CSS_VARS.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 20px;
  background: transparent;

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

const ApplyBtn = styled(Button)`
  margin-right: 10px;
  color: ${CSS_VARS.lighterBlue};
`;

const ComfirmBtn = styled(Button)`
  margin-right: 10px;
`;

const Li = styled.li`
  color: ${CSS_VARS.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 13px;
  padding: 15px 20px;
  border-bottom: 1px solid ${CSS_VARS.lighterGray};

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const Block = styled.div`
  border-bottom: 1px solid ${CSS_VARS.lighterGray};
  padding: 10px;
`;

class HdfsConfiguration extends React.Component {
  static propTypes = {
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    connections: [],
    connectionName: '',
    connectionUrl: '',
    connectionId: '',
    isBtnDisabled: true,
    isFormDirty: false,
    isValidConnection: false,
    isTestBtnWorking: false,
  };

  componentDidMount() {
    this.fetchHdfs();
  }

  fetchHdfs = async () => {
    const res = await configurationApis.fetchHdfs();
    const result = _.get(res, 'data.result', []);

    if (!_.isEmptyArr(result)) {
      this.setState({ connections: result });
    }
  };

  handleAddConnection = e => {
    e.preventDefault();
    const { connections } = this.state;
    this.setState({
      connections: [
        ...connections,
        { name: 'Untitled Connection', id: '', uri: '' },
      ],
    });
  };

  handleDeleteConnection = async (e, id, name) => {
    e.preventDefault();
    if (!name) {
      toastr.error(MESSAGES.CONFIG_DELETE_CHECK);
      return;
    }
    if (!id) {
      this.setState({
        connections: [],
      });
    } else {
      const res = await configurationApis.deleteHdfs(id);
      const isSuccess = _.get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_DELETE_SUCCESS + name);
      }
    }
    this.fetchHdfs();
    this.resetModal();
  };

  handleChange = ({ target: { value, name } }) => {
    this.setState(
      { [name]: value, isFormDirty: true, isBtnDisabled: true },
      () => {
        const { connectionName, connectionUrl } = this.state;
        if (!this.hasEmptyInput([connectionName, connectionUrl])) {
          this.setState({ isBtnDisabled: false });
        }
      },
    );
  };

  handleSave = async e => {
    e.preventDefault();
    const { connectionName: name, connectionUrl: uri } = this.state;
    if (!name) {
      toastr.error(MESSAGES.EMPTY_NAME_ERROR);
      return;
    }
    if (!uri) {
      toastr.error(MESSAGES.EMPTY_CONN_URL_ERROR);
      return;
    }
    const isValid = this.state.isValidConnection;
    if (isValid) {
      const res = await configurationApis.saveHdfs({ name, uri });
      const isSuccess = _.get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
        this.fetchHdfs();
        this.setState({ isModalActive: false, isFormDirty: false });
        this.resetModal();
        this.handleModalClose();
      } else {
        toastr.error(MESSAGES.GENERIC_ERROR);
      }
    } else {
      toastr.error(MESSAGES.TEST_NOT_SUCCESS);
    }
  };

  handleTestConnection = async e => {
    e.preventDefault();
    const { connectionUrl: uri } = this.state;
    this.updateBtn(true);
    const res = await configurationApis.validateHdfs({ uri });
    this.updateBtn(false);

    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDirty: false, isValidConnection: true });
    }
  };

  handleApplyConnection = async e => {
    e.preventDefault();
    const { connectionName: name, connectionUrl: uri } = this.state;
    if (!name) {
      toastr.error(MESSAGES.EMPTY_NAME_ERROR);
      return;
    }
    if (!uri) {
      toastr.error(MESSAGES.EMPTY_CONN_URL_ERROR);
      return;
    }
    const isValid = this.state.isValidConnection;
    if (isValid) {
      const res = await configurationApis.saveHdfs({ name, uri });
      const isSuccess = _.get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
        this.fetchHdfs();
        this.setState({ isModalActive: false, isFormDirty: false });
        this.resetModal();
      } else {
        toastr.error(MESSAGES.GENERIC_ERROR);
      }
    } else {
      toastr.error(MESSAGES.TEST_NOT_SUCCESS);
    }
  };

  hasEmptyInput = inputs => {
    return inputs.some(input => _.isEmptyStr(input));
  };

  updateBtn = update => {
    this.setState({ isTestBtnWorking: update, isBtnDisabled: update });
  };

  resetModal = () => {
    this.setState({
      connectionName: '',
      connectionUrl: '',
      connectionId: '',
    });
  };

  setModalField = (e, id) => {
    const result = this.state.connections.filter(conn => {
      return conn.id === id;
    });

    this.setState({
      connectionName: result[0].name,
      connectionUrl: result[0].uri,
      connectionId: result[0].id,
    });
  };

  handleModalClose = () => {
    this.props.handleClose();
    this.reset();
  };

  reset = () => {
    this.setState({
      connections: [],
      connectionName: '',
      connectionUrl: '',
      connectionId: '',
    });
  };

  render() {
    const {
      connections,
      connectionName,
      connectionUrl,
      connectionId,
      isTestBtnWorking,
    } = this.state;

    return (
      <div style={modalStyles}>
        <ModalBody>
          <Connections>
            <Block>
              <AddIcon onClick={this.handleAddConnection}>
                <i className="far fa-plus-square" />
              </AddIcon>
              <DeleteIcon
                onClick={e =>
                  this.handleDeleteConnection(e, connectionId, connectionName)
                }
              >
                <i className="far fa-minus-square" />
              </DeleteIcon>
            </Block>

            <ul>
              {connections.map(({ id, name }, idx) => {
                return (
                  <Li key={idx} onClick={e => this.setModalField(e, id)}>
                    {name}
                  </Li>
                );
              })}
            </ul>
          </Connections>
          <HdfsForm>
            <FormGroup>
              <Label>Name</Label>
              <Input
                type="text"
                id="connectionName"
                name="connectionName"
                width="250px"
                placeholder="My HDFS"
                value={connectionName}
                data-testid="connection-name-input"
                handleChange={this.handleChange}
              />
            </FormGroup>
            <FormGroup>
              <Label>HDFS connection URL</Label>
              <Input
                id="connectionUrl"
                name="connectionUrl"
                width="250px"
                placeholder="file://"
                value={connectionUrl}
                data-testid="connection-url-input"
                handleChange={this.handleChange}
              />
              <TestBtn
                text="Test Connection"
                theme={defaultBtn}
                isWorking={isTestBtnWorking}
                data-testid="test-connection-btn"
                handleClick={this.handleTestConnection}
              />
            </FormGroup>
          </HdfsForm>
        </ModalBody>
        <div>
          <Actions>
            <CancelBtn
              text="Cancel"
              theme={cancelBtn}
              handleClick={this.handleModalClose}
              data-testid="modal-cancel-btn"
            />
            <ApplyBtn
              text="Apply"
              theme={defaultBtn}
              data-testid="modal-apply-btn"
              handleClick={this.handleApplyConnection}
            />
            <ComfirmBtn
              text="OK"
              theme={primaryBtn}
              handleClick={this.handleSave}
              data-testid="modal-submit-btn"
            />
          </Actions>
        </div>
      </div>
    );
  }
}

export default HdfsConfiguration;
