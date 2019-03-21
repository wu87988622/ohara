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
import { noop, get, isEmpty, sortBy, map, find } from 'lodash';

import * as configurationApi from 'api/configurationApi';
import { Modal } from 'common/Modal';
import { FormGroup, Label, Select } from 'common/Form';
import { Box } from 'common/Layout';
import { LinkButton } from 'common/Form';
import { PreviewWrapper, QuicklyFillInWrapper, Text } from './styles';

class FtpQuicklyFillIn extends React.Component {
  static propTypes = {
    onFillIn: PropTypes.func.isRequired,
    onCancel: PropTypes.func,
  };

  static defaultProps = {
    onCancel: noop,
  };

  state = {
    isModalActive: false,
    isLoading: true,
    ftpConfigurations: [],
    currFtpConfiguration: null,
  };

  fetchData = async () => {
    const res = await configurationApi.fetchFtp();
    const ftpConfigurations = get(res, 'data.result', []);
    this.setState(() => ({ isLoading: false }));
    if (!isEmpty(ftpConfigurations)) {
      this.setState({ ftpConfigurations: sortBy(ftpConfigurations, 'name') });
    }
  };

  handleModalOpen = e => {
    e.preventDefault();
    this.setState(
      {
        isModalActive: true,
        isLoading: false,
      },
      () => {
        this.fetchData();
      },
    );
  };

  handleModalClose = () => {
    this.setState({ isModalActive: false, currFtpConfiguration: null });
    this.props.onCancel();
  };

  handleSelectChange = ({ target }) => {
    const { value: selectedFtpName } = target;
    const { ftpConfigurations } = this.state;
    const ftpConfiguration = find(ftpConfigurations, { name: selectedFtpName });
    this.setState({ currFtpConfiguration: ftpConfiguration });
  };

  handleFillIn = e => {
    e.preventDefault();
    const { currFtpConfiguration } = this.state;
    const values = {
      hostname: currFtpConfiguration.hostname,
      port: String(currFtpConfiguration.port),
      user: currFtpConfiguration.user,
      password: currFtpConfiguration.password,
    };
    this.props.onFillIn(values);
    this.handleModalClose();
  };

  render() {
    const {
      isModalActive,
      ftpConfigurations,
      currFtpConfiguration,
    } = this.state;
    const ftpNames = map(ftpConfigurations, 'name');

    return (
      <QuicklyFillInWrapper>
        <LinkButton handleClick={this.handleModalOpen}>
          Quickly fill in
        </LinkButton>
        <Modal
          title="Quickly fill in"
          isActive={isModalActive}
          width="450px"
          handleCancel={this.handleModalClose}
          handleConfirm={this.handleFillIn}
          confirmBtnText="Fill in"
          isConfirmDisabled={!currFtpConfiguration}
        >
          <Box shadow={false}>
            <FormGroup>
              <Label>FTP Configuration</Label>
              <Select
                name="ftp"
                list={ftpNames}
                selected={get(currFtpConfiguration, 'name', '')}
                handleChange={this.handleSelectChange}
                placeholder="Please select a connection"
              />
            </FormGroup>
            {currFtpConfiguration && (
              <PreviewWrapper>
                <FormGroup isInline>
                  <Label>FTP host:</Label>
                  <Text>{currFtpConfiguration.hostname}</Text>
                </FormGroup>
                <FormGroup isInline>
                  <Label>FTP port:</Label>
                  <Text>{currFtpConfiguration.port}</Text>
                </FormGroup>
                <FormGroup isInline>
                  <Label>User name:</Label>
                  <Text>{currFtpConfiguration.user}</Text>
                </FormGroup>
                <FormGroup isInline>
                  <Label>Password:</Label>
                  <Text>{currFtpConfiguration.password ? '********' : ''}</Text>
                </FormGroup>
              </PreviewWrapper>
            )}
          </Box>
        </Modal>
      </QuicklyFillInWrapper>
    );
  }
}

export default FtpQuicklyFillIn;
