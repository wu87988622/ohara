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
import { map, reject, some } from 'lodash';
import Grid from '@material-ui/core/Grid';

import * as inspectApi from 'api/inspectApi';
import FileCard from 'components/Workspace/Card/FileCard';
import SelectCard from 'components/Workspace/Card/SelectCard';

const FilesField = props => {
  const {
    input: { value: files = [], onChange },
  } = props;

  const handleUpload = async filesToUpload => {
    const res = await inspectApi.getFileInfoWithoutUpload({
      file: filesToUpload[0],
    });
    const uploadedFileInfo = res.data;
    const uploadedFile = { ...uploadedFileInfo, file: filesToUpload[0] };

    // If the name of the newly uploaded file already exists, replace the old one
    const isFileExisted = some(files, file => file.name === uploadedFile.name);
    if (isFileExisted) {
      const remainingFiles = reject(
        files,
        file => file.name === uploadedFile.name,
      );
      onChange([...remainingFiles, uploadedFile]);
    } else {
      onChange([...files, uploadedFile]);
    }
  };

  const handleDelete = fileToDelete => () => {
    const remainingFiles = reject(
      files,
      file => file.name === fileToDelete.name,
    );
    onChange(remainingFiles);
  };

  return (
    <Grid
      container
      direction="row"
      justify="flex-start"
      alignItems="flex-start"
    >
      {map(files, file => (
        <Grid item xs={4} key={file.name}>
          <SelectCard rows={file} handleClose={handleDelete(file)} />
        </Grid>
      ))}
      <Grid item xs={files.length > 0 ? 4 : 12}>
        <FileCard
          handelDrop={handleUpload}
          title="Add worker plugins"
          content="Drop files here or click to select files to upload"
          sm={files.length > 0}
        />
      </Grid>
    </Grid>
  );
};

FilesField.propTypes = {
  input: PropTypes.shape({
    name: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.array.isRequired,
  }).isRequired,
  meta: PropTypes.shape({
    error: PropTypes.string,
    invalid: PropTypes.bool,
    touched: PropTypes.bool,
  }),
};

export default FilesField;
