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

import { useEffect, useState, useRef, useContext } from 'react';
import _ from 'lodash';

import * as hooks from 'hooks';
import { KIND } from 'const';
import { PaperContext } from '../Pipeline';

export const useTopics = () => {
  const topicsDataInToolBox = hooks.useTopicsInToolbox();

  // Pipeline-only topic
  const pipelineOnlyTopic = {
    name: 'Pipeline Only',
    tags: { isShared: false, label: 'Pipeline Only' },
  };

  const topics = [pipelineOnlyTopic, ...topicsDataInToolBox].map(topic => ({
    displayName: topic.displayName || '',
    kind: KIND.topic,
    className: KIND.topic,
    name: topic.name,
    isShared: topic.isShared,
    status: topic.state,
  }));

  return [topics, topicsDataInToolBox];
};

const getStreamsFromFiles = (results, file) => {
  const { group, name, classInfos } = file;
  const streams = classInfos.filter(({ settingDefinitions: defs }) =>
    defs.find(def => def.key === 'kind' && def.defaultValue === KIND.stream),
  );

  if (streams.length > 0) {
    results = results.concat(
      streams.map(({ className }) => ({
        name: className.split('.').pop(),
        kind: KIND.stream,
        jarKey: { group, name },
        className,
      })),
    );
  }

  return results;
};

export const useStreams = () => {
  const files = hooks.useFiles();
  const currentWorkspace = hooks.useWorkspace();

  return files
    .filter(file =>
      currentWorkspace?.stream?.jarKeys.some(
        jarKey => jarKey.name === file.name,
      ),
    )
    .reduce(getStreamsFromFiles, [])
    .sort((a, b) => a.className.localeCompare(b.className));
};

export const useToolboxHeight = ({ expanded, searchResults, connectors }) => {
  const [toolboxHeight, setToolboxHeight] = useState(0);
  const toolboxRef = useRef(null);
  const toolboxHeaderRef = useRef(null);
  const panelSummaryRef = useRef(null);
  const panelAddButtonRef = useRef(null);
  const paperApi = useContext(PaperContext);

  useEffect(() => {
    const paperHeight = paperApi.getBbox().height;
    const toolboxOffsetTop = toolboxRef.current.state.y + 8; // offset top of toolbox
    const toolboxHeaderHeight = toolboxHeaderRef.current.clientHeight;
    const summaryHeight = panelSummaryRef.current.clientHeight * 4; // we have 4 summaries
    const itemHeight = 38; // Since the item is added by JointJS, we cannot get the height, therefore, the hard coded value
    const addButtonHeight = panelAddButtonRef.current.clientHeight;
    const toolbarHeight = 72;

    // When there's search result, we need to use it
    let { sources, topics, streams, sinks } = _.isEmpty(searchResults)
      ? connectors
      : searchResults;

    const panelHeights = {
      source: itemHeight * sources.length + addButtonHeight,
      // +1 for the pipeline-only topic icon
      topic: itemHeight * topics.length + addButtonHeight,
      stream: itemHeight * streams.length + addButtonHeight,
      sink: itemHeight * sinks.length + addButtonHeight,
    };

    const totalHeight = Object.keys(expanded)
      .filter(panel => Boolean(expanded[panel])) // Get expanded panels
      .map(panel => panelHeights[panel])
      .reduce((acc, cur) => acc + cur, summaryHeight + toolboxHeaderHeight);

    if (totalHeight + toolboxOffsetTop > paperHeight) {
      const newHeight =
        paperHeight - toolboxOffsetTop - toolbarHeight - toolboxHeaderHeight;
      return setToolboxHeight(newHeight);
    }

    // Reset, value `0` will remove the scrollbar from Toolbox body
    return setToolboxHeight(0);
  }, [connectors, expanded, paperApi, searchResults]);

  return {
    toolboxHeight,
    toolboxRef,
    toolboxHeaderRef,
    panelSummaryRef,
    panelAddButtonRef,
  };
};
