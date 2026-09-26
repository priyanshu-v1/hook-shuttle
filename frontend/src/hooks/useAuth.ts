import { useMutation } from "@tanstack/react-query";
import { loginUser, RegisterRequest, registerUser } from "@/api/authApi";
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

  const registerMutation = useMutation({
    mutationFn: registerUser,
    onSuccess: (session: Session, variables: RegisterRequest) => {
      localStorage.setItem("hookshuttle.session", JSON.stringify(session));
      toast.success("Admin account created", {
        description: `${variables.organization_name} workspace is ready — you're signed in as ${variables.email}.`,
      });
      navigate({ to: "/dashboard", replace: true });
    },
    onError: (error: any) => {
      toast.error("Account creation failed", { 
        description: error?.response?.data?.message || error?.message || "Could not register workspace" 
      });
    },
  });

  return {
    login: loginMutation.mutateAsync,
    isLoggingIn: loginMutation.isPending,
    register: registerMutation.mutateAsync,
    isRegistering: registerMutation.isPending,
  };
}
