import { useCallback, useEffect, useState } from 'react';

const THEME_STORAGE_KEY = 'shixianwen-theme';
const LIGHT_THEME = 'light';
const DARK_THEME = 'dark';

function readSavedTheme() {
  return window.localStorage.getItem(THEME_STORAGE_KEY) === DARK_THEME ? DARK_THEME : LIGHT_THEME;
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;

  const themeColor = theme === DARK_THEME ? '#121315' : '#ffffff';
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', themeColor);
}

export default function useTheme() {
  const [theme, setTheme] = useState(() => {
    const savedTheme = readSavedTheme();
    applyTheme(savedTheme);
    return savedTheme;
  });

  useEffect(() => {
    applyTheme(theme);
    window.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((currentTheme) => (currentTheme === DARK_THEME ? LIGHT_THEME : DARK_THEME));
  }, []);

  return {
    theme,
    isDarkTheme: theme === DARK_THEME,
    toggleTheme,
  };
}
