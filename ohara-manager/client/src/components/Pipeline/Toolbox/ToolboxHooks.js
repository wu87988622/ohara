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
