import { apiClient, unwrap } from "./client";
import type {
  AdminUser,
  AdminUserCreatePayload,
  AdminUserPasswordPayload,
  AdminUserStatusPayload,
  AdminUserUpdatePayload,
  PageResult
} from "../types";

export interface AdminUserPageQuery {
  page_num?: number;
  page_size?: number;
}

export function fetchAdminUsers(query: AdminUserPageQuery) {
  return unwrap<PageResult<AdminUser>>(apiClient.get("/admin/users", { params: query }));
}

export function fetchAdminUser(id: number) {
  return unwrap<AdminUser>(apiClient.get(`/admin/users/${id}`));
}

export function createAdminUser(payload: AdminUserCreatePayload) {
  return unwrap<AdminUser>(apiClient.post("/admin/users", payload));
}

export function updateAdminUser(id: number, payload: AdminUserUpdatePayload) {
  return unwrap<AdminUser>(apiClient.put(`/admin/users/${id}`, payload));
}

export function updateAdminUserStatus(id: number, payload: AdminUserStatusPayload) {
  return unwrap<AdminUser>(apiClient.patch(`/admin/users/${id}/status`, payload));
}

export function resetAdminUserPassword(id: number, payload: AdminUserPasswordPayload) {
  return unwrap<void>(apiClient.patch(`/admin/users/${id}/password`, payload));
}

export function deleteAdminUser(id: number) {
  return unwrap<void>(apiClient.delete(`/admin/users/${id}`));
}
