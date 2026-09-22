import axios from 'axios';
import { getSession, setSession, signOut } from '@/lib/session';
import { AuthResponse } from './authApi';

// 1. General API Client: Used for standard endpoints
export const apiClient = axios.create({
  baseURL: 'http://localhost:8080',
});

// 2. Auth API Client: Dedicated specifically to auth actions needing the HttpOnly cookie
export const authApi = axios.create({
  baseURL: 'http://localhost:8080',
  withCredentials: true,
});

// 3. Request Interceptor: Dynamically injects the current access token
apiClient.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});


// 4. Concurrency Management Variables
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
};

authApi.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
);

// 5. Response Interceptor: Handles 401 Unauthorized and transparent token rotation
apiClient.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config;

    // Safety Guard: Prevent infinite loops if the refresh endpoint itself fails
    if (originalRequest.url?.includes('/api/v1/auth/refresh')) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return apiClient(originalRequest);
          })
          .catch((err) => Promise.reject(err));
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Calls refresh endpoint (sends HttpOnly cookie automatically via authApi)
        const response = await authApi.post<void, AuthResponse>('/api/v1/auth/refresh', {});
        const newAccessToken = response.token;

        // Patch the existing session with the new token, preserving the email
        const currentSession = getSession();
        if (currentSession) {
          setSession({ ...currentSession, token: newAccessToken });
        } else {
          // Fallback if session somehow didn't exist
          setSession({ email: '', token: newAccessToken });
        }

        processQueue(null, newAccessToken);

        // Retry the original failed request with the new token
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh token failed -> clear session via signOut and redirect to login
        processQueue(refreshError, null);
        signOut();
        window.location.href = `/`
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);