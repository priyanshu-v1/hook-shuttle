import { useMutation } from "@tanstack/react-query";
import { loginUser } from "@/api/authApi";
import { useNavigate } from "@tanstack/react-router";
import { toast } from "sonner";
import type { Session } from "@/lib/session";

export function useAuth() {
  const navigate = useNavigate();

  const loginMutation = useMutation({
    mutationFn: loginUser,
    onSuccess: (session: Session) => {
      localStorage.setItem("hookshuttle.session", JSON.stringify(session));
      toast.success("Signed in successfully");
      navigate({ to: "/dashboard", replace: true });
    },
    onError: (error: any) => {
      toast.error("Sign in failed", { 
        description: error?.response?.data?.message || error?.message || "Invalid credentials" 
      });
    },
  });

  return {
    login: loginMutation.mutateAsync,
    isLoggingIn: loginMutation.isPending,
  };
}
