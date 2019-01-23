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

import * as CSS_VARS from 'theme/variables';
import { H2 } from 'common/Headings';
import HdfsConfiguration from 'components/pages/Configuration/HdfsConfiguration';
import DbConfiguration from 'components/pages/Configuration/DbConfiguration';
import FtpConfiguration from 'components/pages/Configuration/FtpConfiguration';
import ReactModal from 'react-modal';

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

const H2Wrapper = styled(H2)`
  margin: 0;
  padding: 20px;
  border-bottom: 1px solid ${CSS_VARS.lighterGray};
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

const ConfLi = styled.li`
  color: ${({ isSelected }) =>
    isSelected ? CSS_VARS.blue : CSS_VARS.lightBlue};
  border: 0;
  font-size: 14px;
  cursor: pointer;
  padding-top: 20px;
  padding-left: 10px;

  &:hover {
    color: ${({ isSelected }) =>
      isSelected ? CSS_VARS.blue : CSS_VARS.lightBlue};
  }
`;

const ConfUl = styled.ul`
  color: ${({ isSelected }) =>
    isSelected ? CSS_VARS.blue : CSS_VARS.lightBlue};
  border: 0;
  font-size: 14px;
  cursor: pointer;
  padding-top: 20px;

  &:hover {
    color: ${({ isSelected }) =>
      isSelected ? CSS_VARS.blue : CSS_VARS.lightBlue};
  }
`;

const childNames = {
  HDFS_CONFIGURATION: 'HDFS',
  FTP_CONFIGURATION: 'FTP',
  DATABASE_CONFIGURATION: 'DATABASE',
};

class ConfigurationModal extends React.Component {
  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    handleClose: PropTypes.func.isRequired,
  };

  state = {
    childName: '',
  };

  componentDidMount() {
    this.modalChild = React.createRef();
  }

  handleModalClose = () => {
    this.props.handleClose();
    this.reset();
  };

  handleModalOpen = childName => {
    this.setState({ childName: childName });
  };

  handleDefaultModal = () => {
    this.setState({ childName: childNames.DATABASE_CONFIGURATION });
  };

  reset = () => {
    this.setState({
      childName: '',
    });
  };

  render() {
    const { isActive } = this.props;
    const { childName } = this.state;
    const isDatabaseSelected =
      childName === childNames.DATABASE_CONFIGURATION ? true : false;
    const isFtpSelected =
      childName === childNames.FTP_CONFIGURATION ? true : false;
    const isHdfsSelected =
      childName === childNames.HDFS_CONFIGURATION ? true : false;

    return (
      <ReactModal
        isOpen={isActive}
        contentLabel="Modal"
        ariaHideApp={false}
        style={modalStyles}
        onRequestClose={this.handleModalClose}
        onAfterOpen={this.handleDefaultModal}
      >
        <H2Wrapper>Configuration</H2Wrapper>
        <CloseBtn data-testid="close-btn" onClick={this.handleModalClose}>
          <i className="fas fa-times" />
        </CloseBtn>

        <ModalBody>
          <ConfigurationList>
            <ConfUl
              isSelected={isDatabaseSelected || isFtpSelected || isHdfsSelected}
            >
              Connections
              <ConfLi
                isSelected={isDatabaseSelected}
                onClick={() =>
                  this.handleModalOpen(childNames.DATABASE_CONFIGURATION)
                }
              >
                Database
              </ConfLi>
              <ConfLi
                isSelected={isFtpSelected}
                onClick={() =>
                  this.handleModalOpen(childNames.FTP_CONFIGURATION)
                }
              >
                FTP
              </ConfLi>
              <ConfLi
                isSelected={isHdfsSelected}
                onClick={() =>
                  this.handleModalOpen(childNames.HDFS_CONFIGURATION)
                }
              >
                HDFS
              </ConfLi>
            </ConfUl>
            <ConfUl>Kafka</ConfUl>
          </ConfigurationList>

          {childName === childNames.DATABASE_CONFIGURATION ? (
            <DbConfiguration handleClose={this.handleModalClose} />
          ) : null}

          {childName === childNames.FTP_CONFIGURATION ? (
            <FtpConfiguration handleClose={this.handleModalClose} />
          ) : null}

          {childName === childNames.HDFS_CONFIGURATION ? (
            <HdfsConfiguration handleClose={this.handleModalClose} />
          ) : null}
        </ModalBody>
      </ReactModal>
    );
  }
}

export default ConfigurationModal;
