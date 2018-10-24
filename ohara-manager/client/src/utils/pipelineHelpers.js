import { ICON_KEYS } from 'constants/pipelines';

const sourceKeys = Object.keys(ICON_KEYS).reduce((acc, iconKey) => {
  if (iconKey.includes('Source')) {
    acc.push(ICON_KEYS[iconKey]);
  }

  return acc;
}, []);

export const isSource = kind => {
  return sourceKeys.some(sourceKey => kind.includes(sourceKey));
};
