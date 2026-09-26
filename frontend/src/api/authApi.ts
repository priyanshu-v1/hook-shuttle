import { authApi } from "./client";
import { signIn as mockSignIn, type Session } from "@/lib/session";


export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  organization_name: string; // matches backend record field
}

export interface AuthResponse {
  token: string;
  token_type: string;
  email: string;
  role: string;
}

const USE_MOCK = import.meta.env["VITE_USE_MOCK"] === "true";

export async function loginUser(credentials: { email: string; password: string }): Promise<Session> {
  if (USE_MOCK) {
    return mockSignIn(credentials.email);
  }
  
  const response = await authApi.post<AuthResponse, AuthResponse, LoginRequest>("/api/v1/auth/login", credentials);

  return {
    email: response.email || credentials.email,
    token: response.token,
  };
}

export async function registerUser(data: RegisterRequest): Promise<Session> {
  if (USE_MOCK) {
    return mockSignIn(data.email);
  }
  const response = await authApi.post<AuthResponse, AuthResponse, RegisterRequest>("/api/v1/auth/register", data);
  return {
    email: response.email || data.email,
    token: response.token,
  };
}