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
import { map } from 'lodash';

import Grid from '@material-ui/core/Grid';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import AddPluginCard from './AddPluginCard';
import PluginCard from './PluginCard';
import { QuickSearch } from 'components/common/Search';
import { Wrapper } from './PluginListStyles';
import { usePlugins } from 'components/Workspace/Edit/hooks';

const PluginList = () => {
  const plugins = usePlugins();
  const [filteredPlugins, setFilteredPlugins] = useState([]);
  const [checked, setChecked] = useState({
    connector: false,
    others: false,
  });

  const handleChange = name => event => {
    setChecked({ ...checked, [name]: event.target.checked });
  };

  return (
    <Wrapper>
      <Grid container className="filters">
        <QuickSearch
          data={plugins}
          keys={['name']}
          setResults={setFilteredPlugins}
        />
        <FormGroup row className="checkboxes">
          <FormControlLabel
            control={
              <Checkbox
                checked={checked.connector}
                onChange={handleChange('connector')}
                value="connector"
                color="primary"
              />
            }
            label="Connector only"
          />
          <FormControlLabel
            control={
              <Checkbox
                checked={checked.others}
                onChange={handleChange('others')}
                value="others"
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
