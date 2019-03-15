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
import { PreviewWrapper, Text } from './styles';

class HdfsQuicklyFillIn extends React.Component {
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
    hdfsConfigurations: [],
    currHdfsConfiguration: null,
  };

  fetchData = async () => {
    const res = await configurationApi.fetchHdfs();
    const hdfsConfigurations = get(res, 'data.result', []);
    this.setState(() => ({ isLoading: false }));
    if (!isEmpty(hdfsConfigurations)) {
      this.setState({ hdfsConfigurations: sortBy(hdfsConfigurations, 'name') });
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
    this.setState({ isModalActive: false, currHdfsConfiguration: null });
    this.props.onCancel();
  };

  handleSelectChange = ({ target }) => {
    const { value: selectedHdfsName } = target;
    const { hdfsConfigurations } = this.state;
    const hdfsConfiguration = find(hdfsConfigurations, {
      name: selectedHdfsName,
    });
    this.setState({ currHdfsConfiguration: hdfsConfiguration });
  };

  handleFillIn = e => {
    e.preventDefault();
    const { currHdfsConfiguration } = this.state;
    const values = {
      url: currHdfsConfiguration.uri,
    };
    this.props.onFillIn(values);
    this.handleModalClose();
  };

  render() {
    const {
      isModalActive,
      hdfsConfigurations,
      currHdfsConfiguration,
    } = this.state;
    const hdfsNames = map(hdfsConfigurations, 'name');

    return (
      <>
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
          isConfirmDisabled={!currHdfsConfiguration}
        >
          <Box shadow={false}>
            <FormGroup>
              <Label>HDFS Configuration</Label>
              <Select
                name="hdfs"
                list={hdfsNames}
                selected={get(currHdfsConfiguration, 'name', '')}
                handleChange={this.handleSelectChange}
                placeholder="Please select a configuration"
              />
            </FormGroup>
            {currHdfsConfiguration && (
              <PreviewWrapper>
                <FormGroup isInline>
                  <Label>Connection URL:</Label>
                  <Text>{currHdfsConfiguration.uri}</Text>
                </FormGroup>
              </PreviewWrapper>
            )}
          </Box>
        </Modal>
      </>
    );
  }
}

export default HdfsQuicklyFillIn;
