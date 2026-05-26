import { request } from "./http";
import type { ApiEnvelope, UserProfile } from "../types/auth";

export async function getUserProfileApi(): Promise<ApiEnvelope<UserProfile>> {
  return request<UserProfile>({ method: "GET", url: "/sys/user/profile" });
}
