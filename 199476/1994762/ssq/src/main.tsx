import { createRoot } from 'react-dom/client';
import { App } from './App';

void import('./data/forecast-data.json').then(({ default: forecastData }) => {
  createRoot(document.getElementById('root')!).render(<App data={forecastData} />);
});
