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
import { includes, map, some, sortBy } from 'lodash';
import toastr from 'toastr';

import * as jarApis from 'apis/jarApis';
import * as _ from 'utils/commonUtils';

import { Modal } from 'common/Modal';
import { Box } from 'common/Layout';
import * as MESSAGES from 'constants/messages';

import * as s from './Styles';

class PluginSelectModal extends React.Component {
  headers = ['#', 'PLUGIN', ''];

  static propTypes = {
    isActive: PropTypes.bool.isRequired,
    onClose: PropTypes.func.isRequired,
    onConfirm: PropTypes.func.isRequired,
    initPluginIds: PropTypes.arrayOf(PropTypes.string),
  };

  static defaultProps = {
    initPluginIds: [],
  };

  state = {
    initPluginIds: null,
    isLoading: true,
    jars: [],
  };

  static getDerivedStateFromProps(nextProps, prevState) {
    if (nextProps.initPluginIds !== prevState.initPluginIds) {
      return {
        initPluginIds: nextProps.initPluginIds,
        jars: map(prevState.jars, jar => ({
          ...jar,
          checked: includes(nextProps.initPluginIds, jar.id),
        })),
      };
    }
    return null;
  }

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const res = await jarApis.fetchJars();
    this.setState(() => ({ isLoading: false }));
    const jars = _.get(res, 'data.result', null);
    if (!_.isNull(jars)) {
      const sortedJars = sortBy(jars, 'name');
      this.setState({ jars: sortedJars });
    }
  };

  handleClose = () => {
    this.props.onClose();
    this.resetChecked();
  };

  handleConfirm = () => {
    const { jars } = this.state;
    const plugins = jars.reduce((results, jar) => {
      if (jar.checked) {
        results.push(jar);
      }
      return results;
    }, []);
    this.props.onConfirm(plugins);
  };

  handleChecked = ({ target }) => {
    const { value, checked } = target;
    this.setState(state => {
      return {
        jars: state.jars.map(jar => {
          if (value === jar.id) {
            return Object.assign({}, jar, {
              checked,
            });
          }
          return jar;
        }),
      };
    });
  };

  handleRowClick = value => {
    this.setState(state => {
      return {
        jars: state.jars.map(jar => {
          if (value === jar.id) {
            return Object.assign({}, jar, {
              checked: !jar.checked,
            });
          }
          return jar;
        }),
      };
    });
  };

  resetChecked = () => {
    this.setState(state => {
      return {
        jars: map(state.jars, jar => ({
          ...jar,
          checked: includes(state.initPluginIds, jar.id),
        })),
      };
    });
  };

  isDuplicateFilename = filename => {
    const { jars } = this.state;
    return some(jars, jar => filename === jar.name);
  };

  handleFileSelect = e => {
    const file = e.target.files[0];
    if (file) {
      const filename = file.name;
      if (this.isDuplicateFilename(filename)) {
        toastr.error(`This file name is duplicate. '${filename}'`);
        return;
      }

      this.uploadJar(file);
    }
  };

  uploadJar = async file => {
    const res = await jarApis.createJar({ file });
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.PLUGIN_UPLOAD_SUCCESS);
      const jar = _.get(res, 'data.result', null);
      jar.checked = true;
      this.setState(state => {
        const jars = [...state.jars, jar];
        const sortedJars = sortBy(jars, 'name');
        return { jars: sortedJars };
      });
    }
  };

  render() {
    const { jars } = this.state;
    return (
      <Modal
        title="Add plugin"
        isActive={this.props.isActive}
        width="480px"
        handleCancel={this.handleClose}
        handleConfirm={this.handleConfirm}
        confirmBtnText="Add"
        isConfirmDisabled={false}
        showActions={false}
      >
        <Box shadow={false}>
          <s.FileUploadInput onChange={this.handleFileSelect} />
          <s.Table headers={this.headers}>
            {jars.map(jar => (
              <tr
                key={jar.id}
                onClick={() => {
                  this.handleRowClick(jar.id);
                }}
              >
                <td>
                  <s.Checkbox
                    value={jar.id}
                    onChange={this.handleChecked}
                    checked={jar.checked || false}
                  />
                </td>
                <td>{jar.name}</td>
              </tr>
            ))}
          </s.Table>
        </Box>
      </Modal>
    );
  }
}

export default PluginSelectModal;
