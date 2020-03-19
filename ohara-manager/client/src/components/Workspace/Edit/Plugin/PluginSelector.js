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

import { get } from 'lodash';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import CloseIcon from '@material-ui/icons/Close';
import AddIcon from '@material-ui/icons/Add';

import { useAddPluginDialog } from 'context';
import * as hooks from 'hooks';
import { QuickSearch } from 'components/common/Search';
import {
  useCandidatePlugins,
  useSelectedPlugins,
} from 'components/Workspace/Edit/hooks';
import { FileUpload } from '../File';
import {
  StyledDialogTitle,
  StyledDialogContent,
  StyledDialogActions,
  PluginFilter,
  PluginList,
} from './PluginSelectorStyles';
import { someByKey } from 'utils/object';

function PluginSelector() {
  const currentWorker = hooks.useCurrentWorker();
  const workerName = get(currentWorker, 'name');

  const { isOpen, close } = useAddPluginDialog();
  const stageWorker = () => {};

  const candidatePlugins = useCandidatePlugins();
  const selectedPlugins = useSelectedPlugins();
  const [filteredPlugins, setFilteredPlugins] = useState([]);
  const [checked, setChecked] = useState({});

  React.useEffect(() => {
    setChecked(
      candidatePlugins.reduce(
        (checked, plugin) => ({
          ...checked,
          [plugin.name]: someByKey(selectedPlugins, plugin),
        }),
        {},
      ),
    );
  }, [candidatePlugins, selectedPlugins, isOpen]);

  const handleChange = name => event => {
    setChecked({ ...checked, [name]: event.target.checked });
  };

  const handleSave = async () => {
    const selectedPlugins = candidatePlugins.filter(
      plugin => checked[plugin.name] === true,
    );
    const newPluginKeys = selectedPlugins.map(plugin => ({
      group: plugin.group,
      name: plugin.name,
    }));
    await stageWorker({ name: workerName, pluginKeys: newPluginKeys });
    close();
  };

  return (
    <>
      <Dialog open={isOpen} onClose={close} scroll="paper">
        <StyledDialogTitle disableTypography>
          <Typography variant="h3">Plugins</Typography>
          <IconButton className="close-button" onClick={close}>
            <CloseIcon />
          </IconButton>
        </StyledDialogTitle>
        <DialogTitle>
          <PluginFilter>
            <QuickSearch
              data={candidatePlugins}
              keys={['name']}
              setResults={setFilteredPlugins}
              className="plugin-filter"
            />
          </PluginFilter>
        </DialogTitle>

        <StyledDialogContent>
          <PluginList>
            {filteredPlugins.map(plugin => (
              <div key={plugin.name}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={checked[plugin.name]}
                      onChange={handleChange(plugin.name)}
                      value={plugin.name}
                      color="primary"
                    />
                  }
                  label={plugin.name}
                />
              </div>
            ))}
            <>
              <FormControlLabel
                control={
                  <IconButton aria-label="delete">
                    <AddIcon color="primary" />
                  </IconButton>
                }
                label={
                  <FileUpload>
                    <Button color="primary" onClick={close}>
                      Upload New Plugin
                    </Button>
                  </FileUpload>
                }
              />
            </>
          </PluginList>
        </StyledDialogContent>

        <StyledDialogActions>
          <Button onClick={close}>Cancel</Button>
          <Button variant="contained" color="primary" onClick={handleSave}>
            Save
          </Button>
        </StyledDialogActions>
      </Dialog>
    </>
  );
}

export default PluginSelector;
