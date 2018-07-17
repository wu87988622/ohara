export const setUserKey = userKey => (localStorage.userKey = userKey);

export const deleteUserKey = () => delete localStorage.userKey;

export const getUserKey = () => localStorage.userKey;

export const isLoggedin = () => !!localStorage.userKey;
