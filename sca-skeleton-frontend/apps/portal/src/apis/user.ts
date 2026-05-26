import { portalRequest } from "./http";
import type { PortalProfile } from "../stores/auth";

export async function getUserProfileApi(): Promise<PortalProfile> {
  const response = await portalRequest<PortalProfile>({
    method: "GET",
    url: "/sys/user/profile"
  });
  return response.data;
}
