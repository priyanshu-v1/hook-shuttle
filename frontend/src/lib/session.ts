const KEY = "hookshuttle.session";

export interface Session {
  email: string;
  token: string;
}

export function getSession(): Session | null {
  if (typeof window === "undefined") return null;
  try {
    const raw = window.localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Session) : null;
  } catch {
    return null;
  }
}

export function signIn(email: string): Session {
  const session: Session = { email, token: `mock.jwt.${Date.now().toString(36)}` };
  window.localStorage.setItem(KEY, JSON.stringify(session));
  return session;
}

export function signOut() {
  window.localStorage.removeItem(KEY);
}
