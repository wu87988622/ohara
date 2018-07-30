let localStorage = {};

export default {
  setItem(key, value) {
    localStorage[key] = value || '';
  },
  getItem(key) {
    return key in localStorage ? localStorage[key] : null;
  },
  removeItem(key) {
    delete localStorage[key];
  },
  get length() {
    return Object.keys(localStorage).length;
  },
  key(i) {
    var keys = Object.keys(localStorage);
    return keys[i] || null;
  },
};
