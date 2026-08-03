import { LegacyDocument } from './legacy/LegacyDocument';
import { LegacyRuntimeBridge } from './legacy/LegacyRuntimeBridge';
import { NavigationProvider } from './navigation/NavigationContext';

export function App({ data }: { data: unknown }) {
  return (
    <NavigationProvider>
      <LegacyDocument data={data} />
      <LegacyRuntimeBridge data={data} />
    </NavigationProvider>
  );
}
