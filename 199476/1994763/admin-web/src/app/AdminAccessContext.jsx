import { createContext, useContext } from 'react';

const AdminAccessContext = createContext({ admin: null, can: () => false });

export function AdminAccessProvider({ admin, children }) {
  const permissions = new Set(admin?.permissions || []);
  const can = (code) => permissions.has('*') || permissions.has(code);
  return (
    <AdminAccessContext.Provider value={{ admin, can }}>
      {children}
    </AdminAccessContext.Provider>
  );
}

export function useAdminAccess() {
  return useContext(AdminAccessContext);
}

export function Can({ permission, children }) {
  const { can } = useAdminAccess();
  return can(permission) ? children : null;
}
