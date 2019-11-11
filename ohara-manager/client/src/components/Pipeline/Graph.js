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

import React, { useEffect } from 'react';
import * as joint from 'jointjs';

const Graph = () => {
  const renderGraph = () => {
    const graph = new joint.dia.Graph();

    // This variable will be used in the future
    // eslint-disable-next-line
    const paper = new joint.dia.Paper({
      el: document.getElementById('paper'),
      model: graph,
      width: '100%',
      height: '100%',
      gridSize: 10,
      drawGrid: { name: 'dot', args: { color: 'black' } },
      defaultConnectionPoint: { name: 'boundary' },
      background: {
        color: 'rgb(245, 245, 245, .1)',
      },
      linkPinning: false,
    });
  };

  useEffect(() => {
    renderGraph();
  }, []);

  return <div id="paper"></div>;
};

export default Graph;
