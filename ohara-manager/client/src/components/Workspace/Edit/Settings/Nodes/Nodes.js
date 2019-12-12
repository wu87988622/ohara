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
import { map } from 'lodash';

import Grid from '@material-ui/core/Grid';
import { useEditWorkspace } from 'context';
import NodeCard from './NodeCard';

const Nodes = () => {
  const { stagingNodes } = useEditWorkspace();

  return (
    <>
      <Grid container spacing={3}>
        {map(stagingNodes, node => (
          <Grid item xs={6} key={node.hostname}>
            <NodeCard node={node} />
          </Grid>
        ))}
      </Grid>
    </>
  );
};

export default Nodes;
