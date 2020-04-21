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

import { filter, includes, sortBy, values } from 'lodash';
import { createSelector } from 'reselect';
import * as transforms from 'store/transforms';

const getAllEntities = state => state?.entities;

const getNodeEntities = state => state?.entities?.nodes;

const getNamesFormProps = (_, props) => props?.names;

export const getAllNodes = createSelector(
  [getNodeEntities, getAllEntities],
  (nodeEntities, allEntities) => {
    const nodes = sortBy(values(nodeEntities), ['hostname']);
    return transforms.transformNodes(nodes, allEntities);
  },
);

export const getNodesByNames = createSelector(
  [getAllNodes, getNamesFormProps],
  (allNodes, names) =>
    filter(allNodes, node => includes(names, node?.hostname)),
);

export const makeGetNodesByNames = () => getNodesByNames;
