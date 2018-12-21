import React from 'react';
import PropTypes from 'prop-types';
import ReactModal from 'react-modal';
import styled from 'styled-components';
import toastr from 'toastr';

import * as _ from 'utils/commonUtils';
import * as CSS_VARS from 'theme/variables';
import * as MESSAGES from 'constants/messages';
import * as configurationApis from 'apis/configurationApis';
import { Input, Button, FormGroup } from 'common/Form';
import { H2 } from 'common/Headings';
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

const ConfigurationList = styled.div`
  padding: 20px;
`;

const Connections = styled.div`
  padding: 0;
  width: 200px;
  height: 350px;
  overflow-y: auto;
  background-color: ${CSS_VARS.whiteSmoke};
`;

const HdfsForm = styled.div`
  padding: 20px;
`;

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${CSS_VARS.lighterGray};
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

const CloseBtn = styled.div`
  position: absolute;
  right: 22px;
  top: 22px;
  cursor: pointer;
  color: ${CSS_VARS.lightGray};
  padding: 5px;
  transition: ${CSS_VARS.durationNormal} all;

  &:hover {
    transition: ${CSS_VARS.durationNormal} all;
    color: ${CSS_VARS.red};
  }
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

const ConfLi = styled.li`
  color: ${CSS_VARS.lightBlue};
  border: 0;
  font-size: 14px;
  cursor: pointer;
  padding-top: 20px;
  padding-left: 10px;

  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

const ConfUl = styled.ul`
  color: ${CSS_VARS.lightBlue};
  border: 0;
  font-size: 14px;
  cursor: pointer;
  padding-top: 20px;
  &:hover {
    color: ${CSS_VARS.blue};
  }
`;

class ConfigurationModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    connections: [],
    connectionName: '',
    connectionUrl: '',
    connectionUuid: '',
    isBtnDisabled: true,
    isFormDirty: false,
    isValidConnection: false,
    isTestBtnWorking: false,
  };

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
        { name: 'Untitled Connection', uuid: '', uri: '' },
      ],
    });
  };

  handleDeleteConnection = async (e, uuid, name) => {
    e.preventDefault();
    const res = await configurationApis.deleteHdfs(uuid);
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.CONFIG_DELETE_SUCCESS + name);
      this.fetchHdfs();
      this.resetModal();
    }
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
    this.updateBtn(true);
    const res = await configurationApis.validateHdfs({ uri });
    this.updateBtn(false);

    const isSuccess = _.get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDirty: false, isValidConnection: true });
      const { connections } = this.state;
      this.setState({
        connections: [...connections, { name: name, uuid: '', uri: uri }],
      });
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
      connectionUuid: '',
    });
  };

  setModalField = (e, uuid) => {
    const result = this.state.connections.filter(conn => {
      return conn.uuid === uuid;
    });

    this.setState({
      connectionName: result[0].name,
      connectionUrl: result[0].uri,
      connectionUuid: result[0].uuid,
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
      connectionUuid: '',
    });
  };

  render() {
    const { isActive } = this.props;

    const {
      connections,
      connectionName,
      connectionUrl,
      connectionUuid,
      isTestBtnWorking,
    } = this.state;

    return (
      <ReactModal
        isOpen={isActive}
        contentLabel="Modal"
        ariaHideApp={false}
        style={modalStyles}
        onRequestClose={this.handleModalClose}
        onAfterOpen={this.fetchHdfs}
      >
        <H2Wrapper>Configuration</H2Wrapper>
        <CloseBtn data-testid="close-btn" onClick={this.handleModalClose}>
          <i className="fas fa-times" />
        </CloseBtn>

        <ModalBody>
          <ConfigurationList>
            <ConfUl>
              Configuration
              <ConfLi>Database</ConfLi>
              <ConfLi>FTP</ConfLi>
              <ConfLi>HDFS</ConfLi>
            </ConfUl>
            <ConfUl>Kafka</ConfUl>
          </ConfigurationList>

          <Connections>
            <Block>
              <AddIcon onClick={this.handleAddConnection}>
                <i className="far fa-plus-square" />
              </AddIcon>
              <DeleteIcon
                onClick={e =>
                  this.handleDeleteConnection(e, connectionUuid, connectionName)
                }
              >
                <i className="far fa-minus-square" />
              </DeleteIcon>
            </Block>

            <ul>
              {connections.map(({ uuid, name }, idx) => {
                return (
                  <Li key={idx} onClick={e => this.setModalField(e, uuid)}>
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
      </ReactModal>
    );
  }
}

export default ConfigurationModal;
