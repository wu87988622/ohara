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

import React, { useState } from 'react';
import PropTypes from 'prop-types';
import toastr from 'toastr';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import { get, divide, floor } from 'lodash';

import * as jarApi from 'api/jarApi';
import * as MESSAGES from 'constants/messages';
import * as utils from '../WorkspacesDetailPageUtils';
import { Button } from 'components/common/Mui/Form';
import { AlertDialog } from 'components/common/Mui/Dialog';
import { SortTable } from 'components/common/Mui/Table';
import { Main, ActionIcon } from '../styles';
import { StyledLabel, StyledInputFile } from './styles';

const StreamApp = props => {
  const { workspaceName } = props;
  const [jarName, setJarName] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const { jars, fetchJars, loading } = utils.useFetchJars(workspaceName);

  const uploadJar = async file => {
    const res = await jarApi.createJar({
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

  const handleModalOpen = name => {
    setJarName(name);
    setIsModalOpen(true);
  };

  const actionButton = data => {
    const { name } = data;
    return (
      <Tooltip title={`Delete ${name}`} enterDelay={1000}>
        <IconButton data-testid={name} onClick={() => handleModalOpen(name)}>
          <ActionIcon className="fas fa-trash-alt" />
        </IconButton>
      </Tooltip>
    );
  };

  const rows = jars.map(jar => {
    return {
      name: jar.name,
      size: floor(divide(jar.size, 1024), 2),
      lastModified: utils.getDateFromTimestamp(jar.lastModified),
      action: actionButton(jar),
    };
  });

  const handleModalClose = () => {
    setIsModalOpen(false);
  };

  const deleteJar = async params => {
    const res = await jarApi.deleteJar(params);
    const isSuccess = get(res, 'data.isSuccess', false);
    setDeleting(false);

    if (isSuccess) {
      toastr.success(MESSAGES.STREAM_APP_DELETE_SUCCESS);
      handleModalClose();
      fetchJars();
    }
  };

  const handleDelete = e => {
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
        <Button component="span" text="New jar" />
      </StyledLabel>
      <Main>
        <SortTable
          isLoading={loading}
          headRows={headRows}
          rows={rows}
          tableName="streamApp"
        />
      </Main>

      <AlertDialog
        title="Delete jar?"
        content="Are you sure you want to delete this jar? This action cannot be undone!"
        open={isModalOpen}
        handleClose={handleModalClose}
        handleConfirm={handleDelete}
        working={deleting}
      />
    </>
  );
};

StreamApp.propTypes = {
  workspaceName: PropTypes.string.isRequired,
};

export default StreamApp;
