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
import { get } from 'lodash';

import * as _ from 'utils/commonUtils';
import * as MESSAGES from 'constants/messages';
import * as configurationApi from 'api/configurationApi';
import * as validateApi from 'api/validateApi';
import { Input, Button, FormGroup, Label } from 'common/Form';
import { cancelBtn, primaryBtn, defaultBtn } from 'theme/btnTheme';
import { toNumber } from 'lodash';

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
  background-color: ${props => props.theme.whiteSmoke};
`;

const FtpForm = styled.div`
  padding: 20px;
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
  border-top: 1px solid ${props => props.theme.lighterGray};
  justify-content: flex-end;
`;

const TestBtn = styled(Button)`
  margin-top: 20px;
  color: ${props => props.theme.lighterBlue};
  width: 140px;

  /* Prevent button from wrapping if the btn is working and therefore an extra loading icon is displayed */
  white-space: nowrap;
`;

const AddIcon = styled.button`
  color: ${props => props.theme.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 20px;
  background: transparent;

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

const DeleteIcon = styled.button`
  color: ${props => props.theme.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 20px;
  background: transparent;

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

const ApplyBtn = styled(Button)`
  margin-right: 10px;
  color: ${props => props.theme.lighterBlue};
`;

const ComfirmBtn = styled(Button)`
  margin-right: 10px;
`;

const Li = styled.li`
  background-color: ${({ isSelected }) =>
    isSelected ? props => props.theme.white : props => props.theme.whiteSmoke};
  color: ${props => props.theme.lightBlue};
  cursor: pointer;
  border: 0;
  font-size: 13px;
  padding: 15px 20px;
  border-bottom: 1px solid ${props => props.theme.lighterGray};

  &:hover {
    color: ${props => props.theme.blue};
  }
`;

const Block = styled.div`
  border-bottom: 1px solid ${props => props.theme.lighterGray};
  padding: 10px;
`;

const HDiv = styled.div`
  display: flex;
  justify-content: space-between;
`;

class FtpConfiguration extends React.Component {
  static propTypes = {
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    connections: [],
    connectionName: '',
    hostname: '',
    id: '',
    port: '',
    user: '',
    password: '',
    isBtnDisabled: true,
    isFormDirty: false,
    isValidConnection: false,
    isTestBtnWorking: false,
  };

  _isMounted = false;

  componentDidMount() {
    this._isMounted = true;
    this.fetchFtp();
  }

  componentWillUnmount() {
    this._isMounted = false;
  }

  fetchFtp = async () => {
    const res = await configurationApi.fetchFtp();
    const result = get(res, 'data.result', []);

    result.forEach(conn => {
      conn.port = String(conn.port);
      conn.isSelected = false;
    });

    if (!_.isEmptyArr(result) && this._isMounted) {
      this.setState({ connections: result });
    }
  };

  handleAddConnection = e => {
    e.preventDefault();
    const { connections } = this.state;
    this.setState({
      connections: [
        ...connections,
        {
          name: 'Untitled Connection',
          id: '',
          hostname: '',
          port: '',
          user: '',
          password: '',
          isSelected: false,
        },
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
      const res = await configurationApi.deleteFtp(id);
      const isSuccess = get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_DELETE_SUCCESS + name);
      }
    }
    this.fetchFtp();
    this.resetModal();
  };

  handleChange = ({ target: { value, name } }) => {
    this.setState(
      { [name]: value, isFormDirty: true, isBtnDisabled: true },
      () => {
        const { connectionName, hostname } = this.state;
        if (!this.hasEmptyInput([connectionName, hostname])) {
          this.setState({ isBtnDisabled: false });
        }
      },
    );
  };

  handleSave = async e => {
    e.preventDefault();
    const {
      connectionName: name,
      id,
      hostname,
      port,
      user,
      password,
    } = this.state;

    if (!name) {
      toastr.error(MESSAGES.EMPTY_NAME_ERROR);
      return;
    }
    if (!hostname) {
      toastr.error(MESSAGES.EMPTY_HOSTNAME_ERROR);
      return;
    }
    if (!port) {
      toastr.error(MESSAGES.EMPTY_PORT_ERROR);
      return;
    }
    if (!user) {
      toastr.error(MESSAGES.EMPTY_USER_ERROR);
      return;
    }
    if (!password) {
      toastr.error(MESSAGES.EMPTY_PASSWORD_ERROR);
      return;
    }
    const isValid = this.state.isValidConnection;
    if (isValid) {
      let res = null;
      if (!id) {
        res = await configurationApi.saveFtp({
          name,
          hostname,
          port: toNumber(port),
          user,
          password,
        });
      } else {
        res = await configurationApi.updateFtp({
          id,
          name,
          hostname,
          port: toNumber(port),
          user,
          password,
        });
      }
      const isSuccess = get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
        this.fetchFtp();
        this.setState({ isModalActive: false, isFormDirty: false });
        this.resetModal();
        this.handleModalClose();
      } else {
        toastr.error(MESSAGES.GENERIC_ERROR);
      }
    } else {
      toastr.error(MESSAGES.TEST_FAILED_ERROR);
    }
  };

  handleTestConnection = async e => {
    e.preventDefault();
    const { hostname, port, user, password } = this.state;
    this.updateBtn(true);
    const res = await validateApi.validateFtp({
      hostname,
      port,
      user,
      password,
    });
    this.updateBtn(false);

    const isSuccess = get(res, 'data.isSuccess', false);

    if (isSuccess) {
      toastr.success(MESSAGES.TEST_SUCCESS);
      this.setState({ isFormDirty: false, isValidConnection: true });
    }
  };

  handleApplyConnection = async e => {
    e.preventDefault();
    const {
      connectionName: name,
      hostname,
      port,
      user,
      password,
      id,
    } = this.state;

    if (!name) {
      toastr.error(MESSAGES.EMPTY_NAME_ERROR);
      return;
    }
    if (!hostname) {
      toastr.error(MESSAGES.EMPTY_HOSTNAME_ERROR);
      return;
    }
    if (!port) {
      toastr.error(MESSAGES.EMPTY_PORT_ERROR);
      return;
    }
    if (!user) {
      toastr.error(MESSAGES.EMPTY_USER_ERROR);
      return;
    }
    if (!password) {
      toastr.error(MESSAGES.EMPTY_PASSWORD_ERROR);
      return;
    }
    const isValid = this.state.isValidConnection;
    if (isValid) {
      let res = null;
      if (!id) {
        res = await configurationApi.saveFtp({
          name,
          hostname,
          port: toNumber(port),
          user,
          password,
        });
      } else {
        res = await configurationApi.updateFtp({
          id,
          name,
          hostname,
          port: toNumber(port),
          user,
          password,
        });
      }
      const isSuccess = get(res, 'data.isSuccess', false);
      if (isSuccess) {
        toastr.success(MESSAGES.CONFIG_SAVE_SUCCESS);
        this.fetchFtp();
        this.setState({ isModalActive: false, isFormDirty: false });
        this.resetModal();
      } else {
        toastr.error(MESSAGES.GENERIC_ERROR);
      }
    } else {
      toastr.error(MESSAGES.TEST_FAILED_ERROR);
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
      id: '',
      hostname: '',
      port: '',
      user: '',
      password: '',
    });
  };

  setModalField = (e, id) => {
    const result = this.state.connections.filter(conn => {
      return conn.id === id;
    });

    const { connections } = this.state;
    connections.forEach(conn => {
      if (conn.id === id) {
        conn.isSelected = true;
      } else {
        conn.isSelected = false;
      }
    });

    this.setState({
      connections: connections,
      connectionName: result[0].name,
      id: result[0].id,
      hostname: result[0].hostname,
      port: String(result[0].port),
      user: result[0].user,
      password: result[0].password,
    });
    result[0].isSelected = true;
  };

  handleModalClose = () => {
    this.props.handleClose();
    if (this._isMounted) {
      this.reset();
    }
  };

  reset = () => {
    this.setState({
      connections: [],
      id: '',
      connectionName: '',
      hostname: '',
      port: '',
      user: '',
      password: '',
    });
  };

  render() {
    const {
      connections,
      connectionName,
      id,
      hostname,
      port,
      user,
      password,
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
                  this.handleDeleteConnection(e, id, connectionName)
                }
              >
                <i className="far fa-minus-square" />
              </DeleteIcon>
            </Block>

            <ul>
              {connections.map(({ id, name, isSelected }, idx) => {
                return (
                  <Li
                    isSelected={isSelected}
                    key={idx}
                    onClick={e => this.setModalField(e, id)}
                  >
                    {name}
                  </Li>
                );
              })}
            </ul>
          </Connections>
          <FtpForm>
            <FormGroup>
              <Label>Name</Label>
              <Input
                type="text"
                id="connectionName"
                name="connectionName"
                width="250px"
                placeholder="My FTP server"
                value={connectionName}
                data-testid="connection-name-input"
                handleChange={this.handleChange}
              />
            </FormGroup>
            <HDiv>
              <FormGroup>
                <Label>FTP host</Label>
                <Input
                  id="hostname"
                  name="hostname"
                  width="150px"
                  placeholder="localhost"
                  value={hostname}
                  data-testid="hostname-input"
                  handleChange={this.handleChange}
                />
              </FormGroup>
              <FormGroup>
                <Label>FTP port</Label>
                <Input
                  type="text"
                  id="port"
                  name="port"
                  width="80px"
                  placeholder="21"
                  value={port}
                  data-testid="port-input"
                  handleChange={this.handleChange}
                />
              </FormGroup>
            </HDiv>
            <FormGroup>
              <Label>User name</Label>
              <Input
                type="text"
                id="user"
                name="user"
                width="250px"
                placeholder="System admin"
                value={user}
                data-testid="user-input"
                handleChange={this.handleChange}
              />
            </FormGroup>
            <FormGroup>
              <Label>Password</Label>
              <Input
                type="password"
                id="password"
                name="password"
                width="250px"
                value={password}
                data-testid="password-input"
                handleChange={this.handleChange}
              />
            </FormGroup>
            <FormGroup>
              <TestBtn
                text="Test Connection"
                theme={defaultBtn}
                isWorking={isTestBtnWorking}
                data-testid="test-connection-btn"
                handleClick={this.handleTestConnection}
              />
            </FormGroup>
          </FtpForm>
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

export default FtpConfiguration;
