import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { User, AuthResponse, UpdateProfileRequest } from '../types';
import { apiClient } from '../api/client';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, fullName: string, role: string) => Promise<void>;
  logout: () => void;
  updateProfile: (data: UpdateProfileRequest) => Promise<User>;
  refreshCurrentUser: () => Promise<User | null>;
  quickLoginAsBuyer: () => Promise<void>;
  quickLoginAsSeller: () => Promise<void>;
  quickLoginAsAdmin: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(localStorage.getItem('tradetwin_token'));
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const refreshCurrentUser = useCallback(async (): Promise<User | null> => {
    const currentToken = localStorage.getItem('tradetwin_token');
    if (!currentToken) {
      setUser(null);
      return null;
    }

    try {
      const res = await apiClient.get<{ data: User }>('/auth/me');
      if (res.data?.data) {
        const fullUser = res.data.data;
        setUser(fullUser);
        localStorage.setItem('tradetwin_user', JSON.stringify(fullUser));
        return fullUser;
      }
    } catch {
      // If token expired, clear
      localStorage.removeItem('tradetwin_token');
      localStorage.removeItem('tradetwin_user');
      setToken(null);
      setUser(null);
    }
    return null;
  }, []);

  useEffect(() => {
    const savedUser = localStorage.getItem('tradetwin_user');
    if (savedUser && token) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem('tradetwin_user');
      }
    }

    // Refresh profile in background if token exists
    if (token) {
      refreshCurrentUser().finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, [token, refreshCurrentUser]);

  const saveAuthSession = (auth: AuthResponse) => {
    const userData: User = {
      id: auth.id,
      email: auth.email,
      fullName: auth.fullName,
      role: auth.role,
      reputationScore: auth.reputationScore,
      defaultCurrency: auth.defaultCurrency || 'USD',
    };
    setToken(auth.token);
    setUser(userData);
    localStorage.setItem('tradetwin_token', auth.token);
    localStorage.setItem('tradetwin_user', JSON.stringify(userData));
  };

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<{ data: AuthResponse }>('/auth/login', { email, password });
      saveAuthSession(res.data.data);
      // Fetch complete profile details (address, preferences)
      await refreshCurrentUser();
    } finally {
      setIsLoading(false);
    }
  };

  const register = async (email: string, password: string, fullName: string, role: string) => {
    setIsLoading(true);
    try {
      const res = await apiClient.post<{ data: AuthResponse }>('/auth/register', {
        email,
        password,
        fullName,
        role,
      });
      saveAuthSession(res.data.data);
      await refreshCurrentUser();
    } finally {
      setIsLoading(false);
    }
  };

  const updateProfile = async (data: UpdateProfileRequest): Promise<User> => {
    const res = await apiClient.put<{ data: User }>('/users/profile', data);
    const updated = res.data.data;
    setUser(updated);
    localStorage.setItem('tradetwin_user', JSON.stringify(updated));
    return updated;
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('tradetwin_token');
    localStorage.removeItem('tradetwin_user');
  };

  const quickLoginAsBuyer = async () => {
    try {
      await login('buyer@tradetwin.com', 'password123');
    } catch {
      await register('buyer@tradetwin.com', 'password123', 'Alice Walker', 'ROLE_BUYER');
    }
  };

  const quickLoginAsSeller = async () => {
    try {
      await login('seller@tradetwin.com', 'password123');
    } catch {
      await register('seller@tradetwin.com', 'password123', 'QuantumTech Direct', 'ROLE_SELLER');
    }
  };

  const quickLoginAsAdmin = async () => {
    try {
      await login('admin@tradetwin.com', 'password123');
    } catch {
      await register('admin@tradetwin.com', 'password123', 'System Administrator', 'ROLE_ADMIN');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
        updateProfile,
        refreshCurrentUser,
        quickLoginAsBuyer,
        quickLoginAsSeller,
        quickLoginAsAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
