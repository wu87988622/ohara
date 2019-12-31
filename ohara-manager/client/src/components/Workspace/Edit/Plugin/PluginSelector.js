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

import { get, isEmpty, includes, keys, pickBy, pick } from 'lodash';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import CloseIcon from '@material-ui/icons/Close';
import AddIcon from '@material-ui/icons/Add';

import {
  useWorkspace,
  useAddPluginDialog,
  useFileState,
  useWorkerActions,
} from 'context';
import { QuickSearch } from 'components/common/Search';
import { FileUpload } from '../File';
import {
  StyledDialogTitle,
  StyledDialogContent,
  StyledDialogActions,
  FileFilter,
  FileList,
} from './PluginSelectorStyles';

function PluginSelector() {
  const { currentWorker } = useWorkspace();
  const workerName = get(currentWorker, 'name');
  const pluginKeys = get(currentWorker, 'stagingSettings.pluginKeys', []);

  const { isOpen, close } = useAddPluginDialog();
  const { data: files = [] } = useFileState();
  const { stageWorker } = useWorkerActions();

  const [filteredFiles, setFilteredFiles] = useState([]);
  const [checked, setChecked] = useState({});

  React.useEffect(() => {
    if (isEmpty(files)) return;

    setChecked(
      files
        .map(f => f.name)
        .reduce(
          (checked, fileName) => ({
            ...checked,
            [fileName]: pluginKeys.some(
              pluginKey => pluginKey.name === fileName,
            ),
          }),
          {},
        ),
    );
  }, [isOpen, files, pluginKeys]);

  const handleChange = name => event => {
    setChecked({ ...checked, [name]: event.target.checked });
  };

  const handleSave = async () => {
    const checkedKeys = keys(pickBy(checked, value => value === true));
    const newPluginKeys = files
      .filter(file => includes(checkedKeys, file.name))
      .map(file => pick(file, ['group', 'name']));
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
          <FileFilter>
            <QuickSearch
              data={files}
              keys={['name']}
              setResults={setFilteredFiles}
              className="file-filter"
            />
          </FileFilter>
        </DialogTitle>

        <StyledDialogContent>
          <FileList>
            {filteredFiles.map(file => (
              <div key={file.name}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={checked[file.name]}
                      onChange={handleChange(file.name)}
                      value={file.name}
                      color="primary"
                    />
                  }
                  label={file.name}
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
          </FileList>
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
