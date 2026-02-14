import { apiGet } from "@/lib/api-loader";
import type { ModelInfo } from "@/lib/api";

export type AdminModelsLoaderData = ModelInfo[];

export async function adminModelsLoader(): Promise<AdminModelsLoaderData> {
  return apiGet<ModelInfo[]>("/admin/model-registry");
}
