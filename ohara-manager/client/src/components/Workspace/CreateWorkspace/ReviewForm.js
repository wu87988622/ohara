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
import { map, replace } from 'lodash';
import { reduxForm } from 'redux-form';
import Button from '@material-ui/core/Button';
import Paper from '@material-ui/core/Paper';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableRow from '@material-ui/core/TableRow';
import TableCell from '@material-ui/core/TableCell';

import { FORM } from 'const';
import * as hooks from 'hooks';

const ReviewForm = props => {
  const { handleSubmit, previousStep } = props;
  const values = hooks.useReduxFormValues(FORM.CREATE_WORKSPACE);

  return (
    <form onSubmit={handleSubmit}>
      <Paper className="summary">
        <Table>
          <TableBody>
            <TableRow>
              <TableCell>Workspace Name</TableCell>
              <TableCell>{values?.workspace?.name}</TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Node Names</TableCell>
              <TableCell>
                {replace(values?.workspace?.nodeNames, /,/g, ', ')}
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell>Plugins</TableCell>
              <TableCell>
                {map(values?.files, file => file.name).join(', ')}
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Paper>
      <div className="buttons">
        <Button onClick={previousStep}>Back</Button>
        <Button variant="contained" color="primary" onClick={handleSubmit}>
          Next
        </Button>
      </div>
    </form>
  );
};

ReviewForm.propTypes = {
  handleSubmit: PropTypes.func.isRequired,
  previousStep: PropTypes.func.isRequired,
};

export default reduxForm({
  form: FORM.CREATE_WORKSPACE,
  destroyOnUnmount: false,
  forceUnregisterOnUnmount: true,
})(ReviewForm);
