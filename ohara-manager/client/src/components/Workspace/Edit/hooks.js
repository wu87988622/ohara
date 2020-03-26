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

import { useEffect, useMemo } from 'react';
import { get, isEqual, unionWith, some, find } from 'lodash';

import * as hooks from 'hooks';
import { getDateFromTimestamp } from 'utils/date';
import { getKey, isEqualByKey } from 'utils/object';

const getOperation = (object, previousList, currentList) => {
  const inPreviousList = some(previousList, object);
  const inCurrentList = some(currentList, object);
  if (inPreviousList && !inCurrentList) return 'remove';
  if (!inPreviousList && inCurrentList) return 'add';
  return '';
};

export const usePluginKeys = () => {
  const currentWorker = hooks.useWorker();
  const prevKeys = get(currentWorker, 'pluginKeys');
  const currKeys = get(currentWorker, 'stagingSettings.pluginKeys');
  return useMemo(() => {
    const pluginKeys = unionWith(prevKeys, currKeys, isEqual);
    return pluginKeys.map(key => ({
      ...key,
      op: getOperation(key, prevKeys, currKeys),
    }));
  }, [prevKeys, currKeys]);
};

const useFiles = () => {
  const currentWorkspace = hooks.useWorkspace();
  const files = hooks.useFiles();
  const fetchFiles = hooks.useFetchFilesAction();
  const pluginKeys = usePluginKeys();

  useEffect(() => {
    if (!currentWorkspace) return;
    fetchFiles();
  }, [fetchFiles, currentWorkspace]);

  return useMemo(() => {
    return files.map(file => {
      return {
        ...file,
        lastModified: getDateFromTimestamp(get(file, 'lastModified')),
        isUsed: some(pluginKeys, getKey(file)),
      };
    });
  }, [files, pluginKeys]);
};

export const usePlugins = () => {
  const pluginKeys = usePluginKeys();
  const files = useFiles();

  return useMemo(() => {
    return pluginKeys.map(pluginKey => {
      const targetFile = find(files, file => isEqualByKey(file, pluginKey));
      return { ...targetFile, ...pluginKey, status: pluginKey.op };
    });
  }, [pluginKeys, files]);
};

export const useSelectedPlugins = () => {
  const plugins = usePlugins();

  return useMemo(() => {
    return plugins.filter(plugin => {
      return plugin.op !== 'remove';
    });
  }, [plugins]);
};

export const useCandidatePlugins = () => {
  const files = useFiles();
  return useMemo(() => {
    return files;
  }, [files]);
};
