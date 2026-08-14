const EVENT_NAME = 'shixianwen-admin-message';
let lastMessage = { content: '', shownAt: 0 };

function show(type, content, duration = 3000) {
  if (!content) return;
  const now = Date.now();
  if (lastMessage.content === content && now - lastMessage.shownAt < 1000) return;
  lastMessage = { content, shownAt: now };
  window.dispatchEvent(new CustomEvent(EVENT_NAME, { detail: { type, content, duration } }));
}

export const message = {
  success: (content, duration) => show('success', content, duration),
  warning: (content, duration) => show('warning', content, duration),
  error: (content, duration) => show('error', content, duration),
  default: (content, duration) => show('default', content, duration),
};

export { EVENT_NAME };
