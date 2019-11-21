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

import { useEffect, useState } from 'react';

import * as inspectApi from 'api/inspectApi';
import * as fileApi from 'api/fileApi';

export const useConnectors = workspace => {
  const [sources, setSources] = useState([]);
  const [sinks, setSinks] = useState([]);

  useEffect(() => {
    if (!workspace) return;

    const fetchWorkerInfo = async () => {
      const { name, group } = workspace.settings;
      const data = await inspectApi.getWorkerInfo({ name, group });
      data.classInfos.forEach(info => {
        const { className, classType } = info;
        const displayClassName = className.split('.').pop();
        if (info.classType === 'source') {
          setSources(prevState => [
            ...prevState,
            { displayName: displayClassName, classType },
          ]);
          return;
        }

        setSinks(prevState => [
          ...prevState,
          { displayName: displayClassName, classType },
        ]);
      });
    };

    fetchWorkerInfo();
  }, [workspace]);

  return [sources, sinks];
};

export const useFiles = workspace => {
  // We're not filtering out other jars here
  // but it should be done when stream jars
  const [streams, setStreams] = useState([]);
  const [fileNames, setFileNames] = useState([]);
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    if (!workspace || status !== 'loading') return;

    const fetchFiles = async workspaceName => {
      const files = await fileApi.getAll({ group: workspaceName });

      const fileNames = files.map(file => file.name);
      setFileNames(fileNames);

      const streamClasses = files
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
            };
          })
          .sort((a, b) => a.className.localeCompare(b.className));
        setStreams(results);
      }

      setStatus('loaded');
    };

    fetchFiles(workspace.settings.name);
  }, [status, workspace]);

  return { streams, setStatus, fileNames };
};
