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

import { useEffect, useState, useRef } from 'react';
import { isEmpty } from 'lodash';

import * as fileApi from 'api/fileApi';
import { hashKey } from 'utils/object';

export const useFiles = workspace => {
  // We're not filtering out other jars here
  // but it should be done when stream jars
  const [streams, setStreams] = useState([]);
  const [files, setFiles] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    if (!workspace || status !== 'loading') return;
    let didCancel = false;

    const fetchFiles = async () => {
      const result = await fileApi.getAll({ group: hashKey(workspace) });
      if (!didCancel) {
        if (!result.errors) {
          setFiles(result.data);

          const streamClasses = result.data
            .map(file => file.classInfos)
            .flat()
            .filter(cls => cls.classType === 'stream');

          if (streamClasses.length > 0) {
            const results = streamClasses
              .map(({ className, classType }) => {
                const displayName = className.split('.').pop();
                return {
                  displayName,
                  classType,
                  className,
                };
              })
              .sort((a, b) => a.className.localeCompare(b.className));
            setStreams(results);
          }

          setStatus('loaded');
        }
      }
    };

    fetchFiles();

    return () => {
      didCancel = true;
    };
  }, [status, workspace]);

  return { streams, setStatus, files };
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

  useEffect(() => {
    if (!paper) return;

    const paperHeight = paper.getComputedSize().height;
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
  }, [connectors, expanded, paper, searchResults]);

  return {
    toolboxHeight,
    toolboxRef,
    toolboxHeaderRef,
    panelSummaryRef,
    panelAddButtonRef,
  };
};
