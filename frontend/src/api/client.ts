import { getSession } from "@/lib/session";
import axios from "axios";

export const apiClient = axios.create({
  baseURL: import.meta.env["VITE_API_BASE_URL"] || "http://localhost:8080",
  headers: {
    "Content-Type": "application/json",
  },
});

apiClient.interceptors.request.use((config) => {
  const session = getSession();
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`;
  }
  return config;
});

// Automatically unwrap response data and handle global errors if needed
apiClient.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // Later we can plug in the 401 token refresh interceptor here
    return Promise.reject(error);
  }
);

