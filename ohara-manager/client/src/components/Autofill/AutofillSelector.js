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
import { get, find, map, noop } from 'lodash';

import ExpansionPanel from '@material-ui/core/ExpansionPanel';
import ExpansionPanelDetails from '@material-ui/core/ExpansionPanelDetails';
import ExpansionPanelSummary from '@material-ui/core/ExpansionPanelSummary';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Radio from '@material-ui/core/Radio';
import Table from '@material-ui/core/Table';
import TableBody from '@material-ui/core/TableBody';
import TableCell from '@material-ui/core/TableCell';
import TableHead from '@material-ui/core/TableHead';
import TableRow from '@material-ui/core/TableRow';
import ExpandMoreIcon from '@material-ui/icons/ExpandMore';

import { Dialog } from 'components/common/Dialog';
import * as hooks from 'hooks';
import Wrapper from './AutofillSelectorStyles';

const AutofillSelector = props => {
  const { isOpen, onClose, onSubmit } = props;
  const [expanded, setExpanded] = React.useState(false);
  const [selected, setSelected] = React.useState(null);

  const currentWorkspace = hooks.useWorkspace();
  const settingFillings = get(currentWorkspace, 'settingFillings', []);

  const handleChange = name => (_, isExpanded) => {
    setExpanded(isExpanded ? name : false);
    setSelected(isExpanded ? name : null);
  };

  const handleClose = () => {
    setExpanded(false);
    setSelected(null);
    onClose();
  };

  const handleSubmit = () => {
    onSubmit(find(settingFillings, { name: selected }));
    handleClose();
  };

  return (
    <Dialog
      confirmDisabled={!selected}
      confirmText="Autofill"
      handleClose={handleClose}
      handleConfirm={handleSubmit}
      maxWidth="sm"
      open={isOpen}
      title="Select Autofill"
    >
      <Wrapper>
        {settingFillings.map(settingFilling => {
          return (
            <ExpansionPanel
              expanded={expanded === settingFilling.name}
              key={settingFilling.name}
              onChange={handleChange(settingFilling.name)}
            >
              <ExpansionPanelSummary
                expandIcon={<ExpandMoreIcon />}
                aria-controls={`autofill-${settingFilling.name}-content`}
                id={settingFilling.name}
              >
                <FormControlLabel
                  aria-controls={`autofill-${settingFilling.name}-control`}
                  checked={selected === settingFilling.name}
                  control={<Radio color="primary" />}
                  label={settingFilling.displayName}
                  value={settingFilling.name}
                />
              </ExpansionPanelSummary>
              <ExpansionPanelDetails>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Key</TableCell>
                      <TableCell>Value</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {map(settingFilling.settings, setting => (
                      <TableRow key={setting.key}>
                        <TableCell>{setting.key}</TableCell>
                        <TableCell>{setting.value}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </ExpansionPanelDetails>
            </ExpansionPanel>
          );
        })}
      </Wrapper>
    </Dialog>
  );
};

AutofillSelector.propTypes = {
  isOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func,
  onSubmit: PropTypes.func,
};

AutofillSelector.defaultProps = {
  onClose: noop,
  onSubmit: noop,
};

export default AutofillSelector;
