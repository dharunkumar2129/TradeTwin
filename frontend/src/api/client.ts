import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '../types';

const RAW_API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';
const API_BASE_URL = RAW_API_BASE.replace(/\/+$/, '');

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 25000,
});

apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('tradetwin_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

export class ApiRequestError extends Error {
  status?: number;
  details?: string[];

  constructor(message: string, status?: number, details?: string[]) {
    super(message);
    this.name = 'ApiRequestError';
    this.status = status;
    this.details = details;
  }
}

export const extractErrorDetails = (err: unknown): string[] => {
  if (err && typeof err === 'object' && 'details' in err) {
    const details = (err as { details?: unknown }).details;
    if (Array.isArray(details)) {
      return details.filter((d): d is string => typeof d === 'string' && d.trim().length > 0);
    }
  }
  return [];
};

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiResponse<unknown> & { details?: string[] }>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('tradetwin_token');
      localStorage.removeItem('tradetwin_user');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    const message = error.response?.data?.message || error.message || 'An unexpected error occurred';
    const details = Array.isArray(error.response?.data?.details) ? error.response?.data?.details : undefined;
    return Promise.reject(new ApiRequestError(message, error.response?.status, details));
  }
);
