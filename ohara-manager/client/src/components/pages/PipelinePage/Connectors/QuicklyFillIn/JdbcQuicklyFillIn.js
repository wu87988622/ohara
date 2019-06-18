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
import { Modal } from 'components/common/Modal';
import { FormGroup, Label, Select } from 'components/common/Form';
import { Box } from 'components/common/Layout';
import { LinkButton } from 'components/common/Form';
import { PreviewWrapper, QuicklyFillInWrapper, Text } from './styles';

class JdbcQuicklyFillIn extends React.Component {
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
    jdbcConfigurations: [],
    currJdbcConfiguration: null,
  };

  fetchData = async () => {
    const res = await configurationApi.fetchJdbc();
    const jdbcConfigurations = get(res, 'data.result', []);
    this.setState(() => ({ isLoading: false }));
    if (!isEmpty(jdbcConfigurations)) {
      this.setState({ jdbcConfigurations: sortBy(jdbcConfigurations, 'name') });
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
    this.setState({ isModalActive: false, currJdbcConfiguration: null });
    this.props.onCancel();
  };

  handleSelectChange = ({ target }) => {
    const { value: selectedJdbcName } = target;
    const { jdbcConfigurations } = this.state;
    const jdbcConfiguration = find(jdbcConfigurations, {
      name: selectedJdbcName,
    });
    this.setState({ currJdbcConfiguration: jdbcConfiguration });
  };

  handleFillIn = e => {
    e.preventDefault();
    const { currJdbcConfiguration } = this.state;
    const values = {
      url: currJdbcConfiguration.url,
      user: currJdbcConfiguration.user,
      password: currJdbcConfiguration.password,
    };
    this.props.onFillIn(values);
    this.handleModalClose();
  };

  render() {
    const {
      isModalActive,
      jdbcConfigurations,
      currJdbcConfiguration,
    } = this.state;
    const jdbcNames = map(jdbcConfigurations, 'name');

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
          isConfirmDisabled={!currJdbcConfiguration}
        >
          <Box shadow={false}>
            <FormGroup>
              <Label>JDBC Configuration</Label>
              <Select
                name="jdbc"
                list={jdbcNames}
                selected={get(currJdbcConfiguration, 'name', '')}
                handleChange={this.handleSelectChange}
                placeholder="Please select a connection"
              />
            </FormGroup>
            {currJdbcConfiguration && (
              <PreviewWrapper>
                <FormGroup isInline>
                  <Label>JDBC URL:</Label>
                  <Text>{currJdbcConfiguration.url}</Text>
                </FormGroup>
                <FormGroup isInline>
                  <Label>User name:</Label>
                  <Text>{currJdbcConfiguration.user}</Text>
                </FormGroup>
                <FormGroup isInline>
                  <Label>Password:</Label>
                  <Text>
                    {currJdbcConfiguration.password ? '********' : ''}
                  </Text>
                </FormGroup>
              </PreviewWrapper>
            )}
          </Box>
        </Modal>
      </QuicklyFillInWrapper>
    );
  }
}

export default JdbcQuicklyFillIn;
