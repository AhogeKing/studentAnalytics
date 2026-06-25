import { apiClient, unwrap } from "./client";
import type { LoginPayload, RegisterPayload, UserProfile } from "../types";

export function login(payload: LoginPayload) {
  return unwrap<UserProfile>(apiClient.post("/login", payload));
}

export function register(payload: RegisterPayload) {
  return unwrap<void>(apiClient.post("/register", payload));
}

export function logout() {
  return unwrap<void>(apiClient.post("/logout"));
}

export function fetchMe() {
  return unwrap<UserProfile>(apiClient.get("/me"));
}
