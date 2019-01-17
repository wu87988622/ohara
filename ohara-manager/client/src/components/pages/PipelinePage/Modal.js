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
import ReactModal from 'react-modal';
import styled from 'styled-components';

import { Box } from 'common/Layout';
import { Input, Button } from 'common/Form';
import { H2 } from 'common/Headings';
import { cancelBtn, primaryBtn } from 'theme/btnTheme';
import { Link } from 'react-router-dom';
import {
  lightBlue,
  lightGray,
  lighterGray,
  red,
  blue,
  durationNormal,
  white,
} from 'theme/variables';

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

const FormInner = styled.div`
  padding: 20px;
`;

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${lighterGray};
`;

const FormGroup = styled.div`
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;
  padding: 20px;
  &:last-child {
    margin-bottom: 0;
  }
`;

const Actions = styled.div`
  display: flex;
  padding: 15px;
  border-top: 1px solid ${lighterGray};
  justify-content: flex-end;
`;

const Label = styled.label`
  color: ${lightBlue};
  margin-bottom: 20px;
`;

const CancelBtn = styled(Button)`
  margin-right: 10px;
`;

CancelBtn.displayName = 'CancelBtn';

const ApplyBtn = styled(Button)`
  margin-right: 10px;
`;

ApplyBtn.displayName = 'ApplyBtn';

const CloseBtn = styled.div`
  position: absolute;
  right: 22px;
  top: 22px;
  cursor: pointer;
  color: ${lightGray};
  padding: 5px;
  transition: ${durationNormal} all;

  &:hover {
    transition: ${durationNormal} all;
    color: ${red};
  }
`;

CloseBtn.displayName = 'CloseBtn';

const LinkIcon = styled(Link)`
  color: ${lightBlue};

  &:hover {
    color: ${blue};
  }
`;
LinkIcon.displayName = 'LinkIcon';

const AddIcon = styled.button`
  color: ${lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background: ${white};

  &:hover {
    color: ${blue};
  }
`;

AddIcon.displayName = 'AddIcon';

const DeleteIcon = styled.button`
  color: ${lightBlue};
  border: 0;
  font-size: 20px;
  cursor: pointer;
  background: ${white};

  &:hover {
    color: ${blue};
  }
`;

DeleteIcon.displayName = 'DeleteIcon';

const Li = styled.li`
  color: ${lightBlue};
  border: 0;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    color: ${blue};
  }
`;

Li.displayName = 'Li';

const Modal = ({
  isActive,
  handleChange,
  handleTestConnection,
  handleSave,
  handleClose,
  setModalField,
  handleAddConnection,
  handleDeleteConnection,
  handleApplyConnection,
  connectionName,
  connectionUrl,
  connectionId,
  fetchHdfs,
  connections,
}) => {
  return (
    <ReactModal
      isOpen={isActive}
      contentLabel="Modal"
      ariaHideApp={false}
      style={modalStyles}
      onRequestClose={handleClose}
      onAfterOpen={fetchHdfs}
    >
      <H2Wrapper>Configuration</H2Wrapper>
      <CloseBtn data-testid="close-btn" onClick={handleClose}>
        <i className="fas fa-times" />
      </CloseBtn>
      <form>
        <td>
          <td>
            <Box>
              <tr>
                <Link exact activeClassName="active" to="">
                  <span>Configuration</span>
                </Link>
              </tr>
              <tr>
                <Link exact activeClassName="active" to="">
                  <span>Database</span>
                </Link>
              </tr>
              <tr>
                <Link exact activeClassName="active" to="">
                  <span>FTP</span>
                </Link>
              </tr>
              <tr>
                <Link exact activeClassName="active" to="">
                  <span>HDFS</span>
                </Link>
              </tr>
              <tr>
                <Link exact activeClassName="active" to="">
                  <span>Kafka</span>
                </Link>
              </tr>
            </Box>
          </td>
          <td>
            <Box>
              <tr>
                <td className="has-icon">
                  <AddIcon onClick={handleAddConnection}>
                    <i className="far fa-plus-square" />
                  </AddIcon>
                </td>

                <DeleteIcon
                  onClick={e =>
                    handleDeleteConnection(e, connectionId, connectionName)
                  }
                >
                  <i className="far fa-minus-square" />
                </DeleteIcon>
              </tr>
              <ul>
                {connections.map(({ id, name }) => {
                  return (
                    <Li key={id} onClick={e => setModalField(e, id)}>
                      {name}
                    </Li>
                  );
                })}
              </ul>
            </Box>
          </td>
          <td>
            <FormInner>
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
                  handleChange={handleChange}
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
                  handleChange={handleChange}
                />
                <a padding-top="25px">
                  <Button
                    text="Test Connection"
                    width="150px"
                    theme={primaryBtn}
                    data-testid="test-connection-btn"
                    handleClick={handleTestConnection}
                  />
                </a>
              </FormGroup>
            </FormInner>
          </td>
        </td>
        <tr>
          <Actions>
            <CancelBtn
              text="Cancel"
              theme={cancelBtn}
              handleClick={handleClose}
              data-testid="modal-cancel-btn"
            />
            <ApplyBtn
              text="Apply"
              theme={primaryBtn}
              data-testid="modal-apply-btn"
              handleClick={handleApplyConnection}
            />
            <ApplyBtn
              text="OK"
              theme={primaryBtn}
              handleClick={handleSave}
              data-testid="modal-submit-btn"
            />
          </Actions>
        </tr>
      </form>
    </ReactModal>
  );
};

Modal.propTypes = {
  isActive: PropTypes.bool,
  handleChange: PropTypes.func,
  handleTestConnection: PropTypes.func,
  handleSave: PropTypes.func,
  handleClose: PropTypes.func,
  setModalField: PropTypes.func,
  handleAddConnection: PropTypes.func,
  handleDeleteConnection: PropTypes.func,
  handleApplyConnection: PropTypes.func,
  connectionName: PropTypes.string,
  connectionUrl: PropTypes.string,
  connectionId: PropTypes.string,
  fetchHdfs: PropTypes.func,
  connections: PropTypes.array,
};

export default Modal;
