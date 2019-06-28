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

import React, { useEffect, useCallback, useState } from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import moment from 'moment';
import { get } from 'lodash';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';

import * as streamApi from 'api/streamApi';
import * as MESSAGES from 'constants/messages';
import { Button } from 'components/common/Mui/Form';
import { ConfirmModal } from 'components/common/Modal';
import { SortTable } from 'components/common/Mui/Table';
import { Main, ActionIcon } from '../styles';
import { StyledLabel, StyledInputFile } from './styles';

const StreamApp = props => {
  const { workspaceName } = props;
  const [jarName, setJarName] = useState(null);
  const [jars, setJars] = useState([]);
  const [order, setOrder] = useState('asc');
  const [orderBy, setOrderBy] = useState('name');
  const [loading, setLoading] = useState(true);
  const [DeleteRowModalActive, setDeleteRowModalActive] = useState(false);

  const fetchJars = useCallback(async () => {
    const res = await streamApi.fetchJars(workspaceName);
    setJars(get(res, 'data.result', []));
    setLoading(false);
  }, [workspaceName]);

  useEffect(() => {
    fetchJars();
  }, [fetchJars]);

  const uploadJar = async file => {
    const res = await streamApi.uploadJar({
      workerClusterName: workspaceName,
      file,
    });
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_UPLOAD_SUCCESS);
    }
    fetchJars();
  };

  const handleFileSelect = e => {
    const file = e.target.files[0];
    if (e.target.files[0]) {
      uploadJar(file);
    }
  };

  const headRows = [
    { id: 'name', label: 'Jar name' },
    { id: 'size', label: 'File size(KB)' },
    { id: 'lastModified', label: 'Last modified' },
    { id: 'action', label: 'Action', sortable: false },
  ];

  const handleDeleteRowModalOpen = name => {
    setJarName(name);
    setDeleteRowModalActive(true);
  };

  const actionButton = data => {
    const { name } = data;
    return (
      <Tooltip title={`Delete ${name}`} enterDelay={1000}>
        <IconButton
          data-testid="edit-node-icon"
          onClick={() => handleDeleteRowModalOpen(name)}
        >
          <ActionIcon className="fas fa-trash-alt" />
        </IconButton>
      </Tooltip>
    );
  };

  const createData = (name, size, lastModified, action) => {
    return { name, size, lastModified, action };
  };

  const rows = jars.map(jar => {
    return createData(
      jar.name,
      999,
      moment.unix(jar.lastModified / 1000).format('YYYY-MM-DD HH:mm:ss'),
      actionButton(jar),
    );
  });
  const handleRequestSort = (event, property) => {
    const isDesc = orderBy === property && order === 'desc';
    setOrder(isDesc ? 'asc' : 'desc');
    setOrderBy(property);
  };

  const handleDeleteRowModalClose = () => {
    setDeleteRowModalActive(false);
    setJarName(null);
  };

  const deleteJar = async params => {
    const res = await streamApi.deleteJar(params);
    const isSuccess = get(res, 'data.isSuccess', false);
    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_DELETE_SUCCESS);
      handleDeleteRowModalClose();
      fetchJars();
    }
  };

  const handleDeleteClick = e => {
    if (jarName) {
      const params = {
        name: jarName,
        workerClusterName: workspaceName,
      };
      deleteJar(params);
    }
  };

  return (
    <>
      <StyledInputFile
        id="fileInput"
        accept=".jar"
        type="file"
        onChange={handleFileSelect}
      />
      <StyledLabel htmlFor="fileInput">
        <Button component="span" text="new jar" />
      </StyledLabel>
      <Main>
        <SortTable
          isLoading={loading}
          headRows={headRows}
          rows={rows}
          onRequestSort={handleRequestSort}
          order={order}
          orderBy={orderBy}
        />
      </Main>
      <ConfirmModal
        isActive={DeleteRowModalActive}
        title="Delete Jar?"
        confirmBtnText="Yes, Delete this jar"
        cancelBtnText="No, Keep it"
        handleCancel={handleDeleteRowModalClose}
        handleConfirm={handleDeleteClick}
        message="Are you sure you want to delete this jar? This action cannot be undone!"
        isDelete
      />
    </>
  );
};

StreamApp.propTypes = {
  workspaceName: PropTypes.string.isRequired,
};

export default StreamApp;
