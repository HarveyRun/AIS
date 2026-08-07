import { useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { PATH_TO_SCREEN, ROUTES } from '../routes/routeConfig.js';

function tabForPath(pathname) {
  if (pathname.startsWith('/items')) return 'requests';
  if (pathname.startsWith('/messages')) return 'messages';
  if (pathname.startsWith('/profile')) return 'profile';
  return 'home';
}

export default function useAppNavigation() {
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const screen = PATH_TO_SCREEN[pathname] ?? 'login';
  const tab = tabForPath(pathname);

  const go = useCallback(
    (nextScreen) => {
      navigate(ROUTES[nextScreen] ?? ROUTES.login);
      window.scrollTo({ top: 0, behavior: 'auto' });
    },
    [navigate],
  );

  return { screen, tab, go };
}
