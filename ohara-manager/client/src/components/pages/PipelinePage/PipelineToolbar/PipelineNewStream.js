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
import toastr from 'toastr';
import styled from 'styled-components';
import { find, some, endsWith, get, isNull } from 'lodash';

import * as streamApi from 'api/streamApi';
import * as MESSAGES from 'constants/messages';
import Editable from '../Editable';
import { ListLoader } from 'common/Loader';
import { ConfirmModal } from 'common/Modal';
import { createConnector } from '../pipelineUtils/pipelineToolbarUtils';
import { Icon, TableWrapper, Table } from './styles';

const FileUploadWrapper = styled.div`
  margin: 20px 30px;
`;

const LoaderWrapper = styled.div`
  margin: 20px 40px;
`;

const StyledIcon = styled(Icon)`
  font-size: 20px;
`;

class PipelineNewStream extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      params: PropTypes.object.isRequired,
    }).isRequired,
    activeConnector: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
    updateGraph: PropTypes.func.isRequired,
    updateAddBtnStatus: PropTypes.func.isRequired,
  };

  state = {
    pipelineId: null,
    isLoading: true,
    jars: [],
    activeId: null,
    file: null,
    isDeleteRowModalActive: false,
    isTitleEditing: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  fetchData = async () => {
    const { match } = this.props;
    const pipelineId = get(match, 'params.pipelineId', null);
    this.setState({ pipelineId }, () => {
      this.fetchJar();
    });
  };

  handleTrSelect = id => {
    this.setState({ activeId: id });
  };

  handleFileSelect = e => {
    this.setState({ file: e.target.files[0] }, () => {
      const { file } = this.state;
      if (file) {
        const filename = file.name;
        if (!this.validateJarExtension(filename)) {
          toastr.error(
            `This file type is not supported.\n Please select your '.jar' file.`,
          );
          return;
        }

        if (this.isDuplicateTitle(filename)) {
          toastr.error(`This file name is duplicate. '${filename}'`);
          return;
        }
        this.uploadJar(file);
      }
    });
  };

  handleDeleteClick = e => {
    e.preventDefault();
    if (this.state.activeId) {
      this.deleteJar(this.state.activeId);
    }
  };

  handleDeleteRowModalOpen = (e, id) => {
    e.preventDefault();
    this.setState({ isDeleteRowModalActive: true, activeId: id });
  };

  handleDeleteRowModalClose = () => {
    this.setState({ isDeleteRowModalActive: false, activeJar: null });
  };

  handleEditIconClick = () => {
    this.setState({ isTitleEditing: true });
  };

  isDuplicateTitle = (title, excludeMyself = false) => {
    const { jars, activeId } = this.state;
    if (excludeMyself) {
      return some(jars, jar => activeId !== jar.id && title === jar.jarName);
    }
    return some(jars, jar => title === jar.jarName);
  };

  validateJarExtension = jarName => endsWith(jarName, '.jar');

  handleTitleChange = ({ target: { value: title } }) => {
    if (this.isDuplicateTitle(title, true)) {
      toastr.error(
        'The filename is already taken, please choose another name.',
      );
      return;
    }

    const newJarName = title;

    if (!this.validateJarExtension(newJarName)) {
      toastr.error(`Unsupported file.\n The accept file is "jar".`);
      return;
    }

    this.setState(({ jars, activeId }) => {
      return {
        jars: jars.map(jar =>
          jar.id === activeId ? { ...jar, jarName: newJarName } : jar,
        ),
      };
    });
  };

  handleTitleConfirm = async isUpdate => {
    if (isUpdate) {
      const { jars, activeId } = this.state;
      const jar = find(jars, { id: activeId });
      if (jar) {
        this.updateJar(jar.id, jar.jarName);
      }
    }
  };

  fetchJar = async () => {
    const { pipelineId } = this.state;
    const res = await streamApi.fetchJar(pipelineId);
    this.setState(() => ({ isLoading: false }));

    const jars = get(res, 'data.result', null);
    const activeId = get(jars, '[0].id', null);

    if (!isNull(jars)) {
      this.setState({ jars, activeId });
    }

    this.props.updateAddBtnStatus(activeId);
  };

  uploadJar = async file => {
    const { pipelineId } = this.state;
    const res = await streamApi.uploadJar({ pipelineId, file });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_UPLOAD_SUCCESS);
      this.setState({ file: null });
      this.fetchJar();
    }
  };

  updateJar = async (id, newJarName) => {
    const res = await streamApi.updateJarName({
      id: id,
      jarName: newJarName,
    });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_RENAME_SUCCESS);
    }
  };

  deleteJar = async id => {
    const res = await streamApi.deleteJar({ id: id });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_DELETE_SUCCESS);
      this.handleDeleteRowModalClose();
      this.fetchJar();
    }
  };

  update = () => {
    const { activeId, jars } = this.state;
    const activeJar = jars.find(jar => jar.id === activeId);
    const connector = {
      ...activeJar,
      className: 'streamApp',
      typeName: 'streamApp',
    };

    const { updateGraph } = this.props;
    createConnector({ updateGraph, connector });
  };

  render() {
    const { isLoading, jars, activeId } = this.state;

    return (
      <div>
        {isLoading ? (
          <LoaderWrapper>
            <ListLoader />
          </LoaderWrapper>
        ) : (
          <React.Fragment>
            <FileUploadWrapper>
              <input
                type="file"
                accept=".jar"
                onChange={this.handleFileSelect}
              />
            </FileUploadWrapper>
            <TableWrapper>
              <Table headers={['FILENAME', 'RENAME', 'DELETE']}>
                {jars.map(({ id, jarName: title }) => {
                  const isActive = id === activeId ? 'is-active' : '';
                  return (
                    <tr
                      className={isActive}
                      key={id}
                      onClick={() => this.handleTrSelect(id)}
                      data-testid="stream-app-item"
                    >
                      <td>
                        <Editable
                          title={title}
                          handleFocusOut={this.handleTitleConfirm}
                          handleChange={this.handleTitleChange}
                          showIcon={false}
                        />
                      </td>
                      <td>
                        <StyledIcon
                          className="far fa-edit"
                          onClick={() => this.handleEditIconClick(id)}
                        />
                      </td>
                      <td>
                        <StyledIcon
                          className="far fa-trash-alt"
                          data-testid="delete-stream-app"
                          onClick={e => this.handleDeleteRowModalOpen(e, id)}
                        />
                      </td>
                    </tr>
                  );
                })}
              </Table>
            </TableWrapper>
          </React.Fragment>
        )}
        <ConfirmModal
          isActive={this.state.isDeleteRowModalActive}
          title="Delete row?"
          confirmBtnText="Yes, Delete this row"
          cancelBtnText="No, Keep it"
          handleCancel={this.handleDeleteRowModalClose}
          handleConfirm={this.handleDeleteClick}
          message="Are you sure you want to delete this row? This action cannot be redo!"
          isDelete
        />
      </div>
    );
  }
}

export default PipelineNewStream;
