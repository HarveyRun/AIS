export const ROUTES = {
  login: '/',
  register: '/register',
  home: '/home',
  talent: '/talents/profile',
  knowledge: '/discover/problems',
  filtered: '/discover/problems/results',
  filter: '/discover/categories',
  apply: '/items/apply',
  requests: '/items',
  createMatter: '/items/new',
  matter: '/items/detail',
  rating: '/items/rating',
  messages: '/messages',
  chat: '/messages/group',
  profile: '/profile',
  settings: '/profile/settings',
  wallet: '/profile/wallet',
  rules: '/profile/rules',
  certs: '/profile/certifications',
  certApply: '/profile/certifications/apply',
  certUpload: '/profile/certifications/materials',
  feedback: '/profile/feedback',
  notices: '/notices',
};

export const PATH_TO_SCREEN = Object.fromEntries(
  Object.entries(ROUTES).map(([screen, route]) => [route, screen]),
);
