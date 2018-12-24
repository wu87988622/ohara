import React from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import styled from 'styled-components';
import { Facebook } from 'react-content-loader';
import { find, some, endsWith } from 'lodash';

import * as _ from 'utils/commonUtils';
import * as CSS_VARS from 'theme/variables';
import * as streamApis from 'apis/streamApis';
import { DataTable } from 'common/Table';
import { ConfirmModal } from 'common/Modal';
import Editable from './Editable';
import * as MESSAGES from 'constants/messages';

const JAR_EXTENSION = '.jar';

const FileUploadWrapper = styled.div`
  margin: 20px 30px;
`;

const TableWrapper = styled.div`
  margin: 20px 30px 40px;
`;

const Table = styled(DataTable)`
  thead th {
    color: ${CSS_VARS.lightBlue};
    font-weight: normal;
  }

  td {
    color: ${CSS_VARS.lighterBlue};
  }

  tbody tr {
    cursor: pointer;
  }

  .is-active {
    background-color: ${CSS_VARS.trBgColor};
  }
`;

const Icon = styled.i`
  color: ${CSS_VARS.lighterBlue};
  font-size: 25px;
  margin-right: 20px;
  transition: ${CSS_VARS.durationNormal} all;
  cursor: pointer;

  &:hover,
  &.is-active {
    transition: ${CSS_VARS.durationNormal} all;
    color: ${CSS_VARS.blue};
  }

  &:last-child {
    border-right: none;
    margin-right: 0;
  }
`;

Icon.displayName = 'Icon';

class PipelineNewStream extends React.Component {
  static propTypes = {
    match: PropTypes.shape({
      isExact: PropTypes.bool,
      params: PropTypes.object,
      path: PropTypes.string,
      url: PropTypes.string,
    }).isRequired,
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
    const pipelineId = _.get(match, 'params.pipelineId', null);
    this.setState({ pipelineId }, () => {
      this.fetchJars();
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

  handleEditIconClick = id => {
    this.setState({ isTitleEditing: true });
  };

  isDuplicateTitle = (title, excludeMyself = false) => {
    const { jars, activeId } = this.state;
    if (excludeMyself) {
      return some(jars, jar => activeId !== jar.id && title === jar.jarName);
    }
    return some(jars, jar => title === jar.jarName);
  };

  validateJarExtension = jarName => endsWith(jarName, JAR_EXTENSION);

  handleTitleChange = ({ target: { value: title } }) => {
    if (this.isDuplicateTitle(title, true)) {
      toastr.error(
        'The filename is already taken, please choose another name.',
      );
      return;
    }

    const newJarName = title;

    if (!this.validateJarExtension(newJarName)) {
      toastr.error(
        `This file type is not supported.\n The file type must be 'jar'.`,
      );
      return;
    }

    this.setState(({ jars, activeId }) => {
      return {
        jars: jars.map(
          jar => (jar.id === activeId ? { ...jar, jarName: newJarName } : jar),
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

  fetchJars = async () => {
    const { pipelineId } = this.state;
    const res = await streamApis.fetchStreamJars(pipelineId);
    this.setState(() => ({ isLoading: false }));

    const result = _.get(res, 'data.result', null);

    if (!_.isNull(result)) {
      this.setState({ jars: result });
    }
  };

  uploadJar = async file => {
    const { pipelineId } = this.state;
    const res = await streamApis.createStreamJar({ pipelineId, file });
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_UPLOAD_SUCCESS);
      this.setState({ file: null });
      this.fetchJars();
    }
  };

  updateJar = async (id, newJarName) => {
    const res = await streamApis.updateStreamJar({
      id: id,
      jarName: newJarName,
    });
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_RENAME_SUCCESS);
    }
  };

  deleteJar = async id => {
    const res = await streamApis.deleteStreamJar({ id: id });
    const isSuccess = _.get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_DELETE_SUCCESS);
      this.handleDeleteRowModalClose();
      this.fetchJars();
    }
  };

  update = () => {
    // TODO:
  };

  render() {
    const { isLoading, jars, activeId } = this.state;

    return (
      <div>
        {isLoading ? (
          <Facebook style={{ width: '70%', height: 'auto' }} />
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
                        <Icon
                          className="fas fa-edit"
                          onClick={() => {
                            this.handleEditIconClick(id);
                          }}
                        />
                      </td>
                      <td>
                        <Icon
                          className="fas fa-trash-alt"
                          onClick={e => {
                            this.handleDeleteRowModalOpen(e, id);
                          }}
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
