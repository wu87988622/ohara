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
import { isEmpty } from 'lodash';

import { KIND, CELL_STATUS } from 'const';
import { useTopicState, useFileActions, useFileState } from 'context';
import { PaperContext } from '../Pipeline';

export const useTopics = () => {
  const { data: topicsData } = useTopicState();

  // Pipeline-only topic
  const pipelineOnlyTopic = {
    name: 'Pipeline Only',
    tags: { isShared: false, label: 'Pipeline Only' },
  };

  const topics = [pipelineOnlyTopic, ...topicsData]
    .map(topic => ({
      displayName: topic.tags.displayName || '',
      kind: KIND.topic,
      className: KIND.topic,
      name: topic.name,
      isShared: topic.tags.isShared,
      status: topic.state,
    }))
    .filter(
      // Some type of topics are hidden from the Toolbox, this including:
      // 1. Pipeline-only topics
      // 2. "Shared" topics that are not running
      topic => {
        const { name, isShared, status } = topic;
        const topicStatus =
          typeof status === 'string' ? status.toLowerCase() : status;

        return (
          name === 'Pipeline Only' ||
          (isShared && topicStatus === CELL_STATUS.running)
        );
      },
    );

  return [topics, topicsData];
};

export const useFiles = () => {
  const [streams, setStreams] = useState([]);

  const { fetchFiles } = useFileActions();
  const { data: files } = useFileState();
  useEffect(() => {
    const loadFiles = async () => {
      await fetchFiles();
      const streamClasses = files.map(file => {
        return {
          name: file.name,
          group: file.group,
          classInfos: file.classInfos.filter(
            classInfo =>
              classInfo.settingDefinitions.find(def => def.key === 'kind')
                .defaultValue === KIND.stream,
          ),
        };
      });
      if (streamClasses.length > 0) {
        const results = streamClasses
          .map(streamClass => {
            return streamClass.classInfos.map(classInfo => {
              const name = classInfo.className.split('.').pop();
              return {
                name,
                kind: KIND.stream,
                className: classInfo.className,
                jarKey: {
                  name: streamClass.name,
                  group: streamClass.group,
                },
              };
            });
          })
          .sort((a, b) => a[0].className.localeCompare(b[0].className))
          .reduce((acc, cur) => acc.concat(cur), []);
        setStreams(results);
      }
    };

    loadFiles();
  }, [fetchFiles, files]);

  return { streams, files };
};

export const useToolboxHeight = ({
  expanded,
  paper,
  searchResults,
  connectors,
}) => {
  const [toolboxHeight, setToolboxHeight] = useState(0);
  const toolboxRef = useRef(null);
  const toolboxHeaderRef = useRef(null);
  const panelSummaryRef = useRef(null);
  const panelAddButtonRef = useRef(null);
  const paperApi = useContext(PaperContext);

  useEffect(() => {
    const paperHeight = paperApi.getBbox().height;
    const toolboxOffsetTop = toolboxRef.current.state.y + 8; // 8 is the offset top of toolbox
    const toolboxHeaderHeight = toolboxHeaderRef.current.clientHeight;
    const summaryHeight = panelSummaryRef.current.clientHeight * 4; // we have 4 summaries
    const itemHeight = 40; // The item is added by JointJS, we cannot get the height thus hard coded
    const addButtonHeight = panelAddButtonRef.current.clientHeight;
    const toolbarHeight = 72;

    // When there's search result, we need to use it
    let { sources, topics, streams, sinks } = isEmpty(searchResults)
      ? connectors
      : searchResults;

    const panelHeights = {
      source: itemHeight * sources.length,
      // +1 for the pipeline-only topic icon
      topic: itemHeight * topics.length + addButtonHeight,
      stream: itemHeight * streams.length + addButtonHeight,
      sink: itemHeight * sinks.length,
    };

    const totalHeight = Object.keys(expanded)
      .filter(panel => Boolean(expanded[panel])) // Get active panels
      .map(panel => panelHeights[panel])
      .reduce((acc, cur) => acc + cur, summaryHeight + toolboxHeaderHeight);

    if (totalHeight + toolboxOffsetTop > paperHeight) {
      const newHeight =
        paperHeight - toolboxOffsetTop - toolbarHeight - toolboxHeaderHeight;
      return setToolboxHeight(newHeight);
    }

    // Reset, value `0` would remove the scrollbar from Toolbox body
    return setToolboxHeight(0);
  }, [connectors, expanded, paper, paperApi, searchResults]);

  return {
    toolboxHeight,
    toolboxRef,
    toolboxHeaderRef,
    panelSummaryRef,
    panelAddButtonRef,
  };
};
