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

import { KIND } from 'const';
import { useTopicState, useFileActions, useFileState } from 'context';
import { PaperContext } from '../Pipeline';

export const useTopics = () => {
  const { data: topicsData } = useTopicState();

  // Private topic
  const privateTopic = {
    name: 'Pipeline Only',
    tags: { type: 'private', label: 'Pipeline Only' },
  };

  const topics = [privateTopic, ...topicsData]
    .map(topic => ({
      classType: KIND.topic,
      name: topic.name,
      type: topic.tags.type,
      displayName: topic.tags.displayName || '',
    }))
    .filter(
      // Private topics are hidden from Toolbox
      topic => topic.type === 'shared' || topic.name === 'Pipeline Only',
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
            classInfo => classInfo.classType === KIND.stream,
          ),
        };
      });
      if (streamClasses.length > 0) {
        const results = streamClasses
          .map(streamClass => {
            return streamClass.classInfos.map(calssInfo => {
              const name = calssInfo.className.split('.').pop();
              return {
                name,
                classType: calssInfo.classType,
                className: calssInfo.className,
                jarKey: {
                  name: streamClass.name,
                  group: streamClass.group,
                },
              };
            });
          })
          .sort((a, b) => a.className.localeCompare(b.className))
          .flat();
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
      source: itemHeight * sources.length + addButtonHeight,
      // +1 for the private topic icon
      topic: itemHeight * topics.length + addButtonHeight,
      stream: itemHeight * streams.length + addButtonHeight,
      sink: itemHeight * sinks.length + addButtonHeight,
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
