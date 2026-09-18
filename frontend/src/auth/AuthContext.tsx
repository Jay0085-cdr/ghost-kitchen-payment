import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { api, clearToken, getToken, setToken } from "../api/client";
import type { AuthResponse, UserRole } from "../types";

interface CurrentUser {
  userId: string;
  organizationId: string;
  email: string;
  role: UserRole;
}

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (organizationName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const USER_KEY = "ghost_kitchen_user";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getToken();
    const storedUser = localStorage.getItem(USER_KEY);
    if (token && storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  function applyAuthResponse(response: AuthResponse) {
    setToken(response.token);
    const currentUser: CurrentUser = {
      userId: response.userId,
      organizationId: response.organizationId,
      email: response.email,
      role: response.role,
    };
    localStorage.setItem(USER_KEY, JSON.stringify(currentUser));
    setUser(currentUser);
  }

  async function login(email: string, password: string) {
    const response = await api.post<AuthResponse>("/api/auth/login", { email, password });
    applyAuthResponse(response);
  }

  async function register(organizationName: string, email: string, password: string) {
    const response = await api.post<AuthResponse>("/api/auth/register", {
      organizationName,
      email,
      password,
    });
    applyAuthResponse(response);
  }

  function logout() {
    clearToken();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within an AuthProvider");
  return context;
}
