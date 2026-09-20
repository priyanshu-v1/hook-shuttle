import { apiClient } from "./client";
import { signIn as mockSignIn, type Session } from "@/lib/session";


export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  role: string;
}

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";

export async function loginUser(credentials: { email: string; password: string }): Promise<Session> {
  if (!USE_MOCK) {
    return mockSignIn(credentials.email);
  }
  
  const response = await apiClient.post<LoginRequest, AuthResponse>("/api/v1/auth/login", credentials);

  return {
    email: response.email || credentials.email,
    token: response.token,
  };
}