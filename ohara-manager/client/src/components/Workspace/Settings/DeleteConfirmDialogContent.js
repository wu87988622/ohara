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

import TextField from '@material-ui/core/TextField';
import Typography from '@material-ui/core/Typography';

const DeleteConfirmDialogContent = ({ workspace, onValidate }) => {
  const handleChange = event => {
    if (event?.target?.value) {
      onValidate(event.target.value === workspace.name);
    }
  };

  return (
    <>
      <Typography paragraph>
        This action cannot be undone. This will permanently delete the{' '}
        {workspace.name} zookeeper, broker, worker, and pipelines.
      </Typography>
      <Typography paragraph>
        Please type <b>{workspace.name}</b> to confirm.
      </Typography>
      <TextField
        autoFocus
        fullWidth
        onChange={handleChange}
        placeholder={workspace.name}
        type="input"
        variant="outlined"
      />
    </>
  );
};

DeleteConfirmDialogContent.propTypes = {
  workspace: PropTypes.shape({
    name: PropTypes.string.isRequired,
  }).isRequired,
  onValidate: PropTypes.func.isRequired,
};

export default DeleteConfirmDialogContent;
