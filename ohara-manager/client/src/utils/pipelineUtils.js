import { ICON_KEYS } from 'constants/pipelines';

const getKeys = kind => {
  return Object.keys(ICON_KEYS).reduce((acc, iconKey) => {
    if (iconKey.includes(kind)) {
      acc.push(ICON_KEYS[iconKey]);
    }

    return acc;
  }, []);
};

const sourceKeys = getKeys('Source');
const sinkKeys = getKeys('Sink');

export const isSource = kind => {
  return sourceKeys.some(sourceKey => kind.includes(sourceKey));
};

export const isSink = kind => {
  return sinkKeys.some(sinkKey => kind.includes(sinkKey));
};
