import { useEffect, useState } from "react";
import { useLoaderData } from "react-router-dom";
import type { AdminModelsLoaderData } from "@/router";
import { useActivateModelMutation } from "@/lib/hooks";
import { AdminModelsScreen } from "@/widgets/admin-models/admin-models-screen";

export default function AdminModelsPage() {
  const data = useLoaderData<AdminModelsLoaderData>();
  const [models, setModels] = useState<AdminModelsLoaderData>(data ?? []);

  useEffect(() => {
    setModels(data ?? []);
  }, [data]);

  const activateMutation = useActivateModelMutation();

  const onActivate = async (modelTag: string) => {
    await activateMutation.mutateAsync({ modelTag });
    setModels((prevModels) =>
      prevModels.map((model) => ({
        ...model,
        active: model.modelTag === modelTag,
        activatedAt: model.modelTag === modelTag ? new Date().toISOString() : model.activatedAt,
      }))
    );
  };

  return (
    <AdminModelsScreen
      models={models}
      activatingModelTag={activateMutation.variables?.modelTag}
      onActivate={onActivate}
    />
  );
}
