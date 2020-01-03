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

import React, { useState, useMemo } from 'react';
import { filter, isEmpty, map, some } from 'lodash';

import Grid from '@material-ui/core/Grid';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import AddPluginCard from './AddPluginCard';
import PluginCard from './PluginCard';
import { QuickSearch } from 'components/common/Search';
import { Wrapper } from './PluginListStyles';
import { usePlugins } from 'components/Workspace/Edit/hooks';

import { someByKey } from 'utils/object';

const CHECKBOXES = {
  CONNECTOR: 'connector',
  STREAM: 'stream',
  OTHERS: 'others',
};

const PluginList = () => {
  const plugins = usePlugins();
  const [checked, setChecked] = useState({
    [CHECKBOXES.CONNECTOR]: false,
    [CHECKBOXES.STREAM]: false,
    [CHECKBOXES.OTHERS]: false,
  });
  const [searchResult, setSearchResult] = useState([]);

  const filteredPlugins = useMemo(() => {
    if (isEmpty(plugins)) return;

    const predicateBySearch = plugin => someByKey(searchResult, plugin);
    const predicateByCheckbox = plugin => {
      if (some(checked)) {
        const { classInfos } = plugin;
        if (
          checked[CHECKBOXES.CONNECTOR] &&
          some(
            classInfos,
            classInfo =>
              classInfo.classType === 'source' ||
              classInfo.classType === 'sink',
          )
        ) {
          return true;
        } else if (
          checked[CHECKBOXES.STREAM] &&
          some(classInfos, classInfo => classInfo.classType === 'stream')
        ) {
          return true;
        } else if (checked[CHECKBOXES.OTHERS] && isEmpty(classInfos)) {
          return true;
        } else {
          return false;
        }
      }
      return true;
    };

    return filter(
      plugins,
      plugin => predicateBySearch(plugin) && predicateByCheckbox(plugin),
    );
  }, [plugins, checked, searchResult]);

  const handleSearchChange = result => {
    setSearchResult(result);
  };

  const handleCheckboxChange = name => event => {
    setChecked({ ...checked, [name]: event.target.checked });
  };

  return (
    <Wrapper>
      <Grid container className="filters">
        <QuickSearch
          data={plugins}
          keys={['name']}
          setResults={handleSearchChange}
        />
        <FormGroup row className="checkboxes">
          <FormControlLabel
            control={
              <Checkbox
                checked={checked[CHECKBOXES.CONNECTOR]}
                onChange={handleCheckboxChange(CHECKBOXES.CONNECTOR)}
                value={CHECKBOXES.CONNECTOR}
                color="primary"
              />
            }
            label="Connector"
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={checked[CHECKBOXES.STREAM]}
                onChange={handleCheckboxChange(CHECKBOXES.STREAM)}
                value={CHECKBOXES.STREAM}
                color="primary"
              />
            }
            label="Stream"
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={checked[CHECKBOXES.OTHERS]}
                onChange={handleCheckboxChange(CHECKBOXES.OTHERS)}
                value={CHECKBOXES.OTHERS}
                color="primary"
              />
            }
            label="Others"
          />
        </FormGroup>
      </Grid>
      <Grid container spacing={3}>
        <Grid item xs={6}>
          <AddPluginCard />
        </Grid>
        {map(filteredPlugins, plugin => (
          <Grid item xs={6} key={plugin.name}>
            <PluginCard plugin={plugin} />
          </Grid>
        ))}
      </Grid>
    </Wrapper>
  );
};

export default PluginList;
