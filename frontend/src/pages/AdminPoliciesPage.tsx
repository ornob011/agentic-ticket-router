import { useLoaderData } from "react-router-dom";
import { useEffect, useMemo, useState } from "react";
import type { AdminPoliciesLoaderData } from "@/router";
import { useResetPolicyMutation, useTogglePolicyStatusMutation, useUpdatePolicyMutation } from "@/lib/hooks";
import { AdminPoliciesScreen } from "@/widgets/admin-policies/admin-policies-screen";

export default function AdminPoliciesPage() {
  const data = useLoaderData<AdminPoliciesLoaderData>();
  const policies = useMemo(() => data ?? [], [data]);

  const [values, setValues] = useState<Record<string, string>>({});
  const [valueErrors, setValueErrors] = useState<Record<string, string>>({});
  const [loadingKey, setLoadingKey] = useState<string | null>(null);

  useEffect(() => {
    setValues(
      Object.fromEntries(
        policies.map((policy) => [policy.configKey, String(policy.configValue)])
      )
    );
    setValueErrors({});
  }, [policies]);

  const saveMutation = useUpdatePolicyMutation();
  const toggleStatusMutation = useTogglePolicyStatusMutation();
  const resetMutation = useResetPolicyMutation();

  const onSave = async (configKey: string, value: string) => {
    const configValue = Number(value);
    if (Number.isNaN(configValue)) {
      setValueErrors((prev) => ({ ...prev, [configKey]: "Value must be a number" }));
      return;
    }

    setValueErrors((prev) => ({ ...prev, [configKey]: "" }));
    setLoadingKey(configKey);
    try {
      await saveMutation.mutateAsync({ configKey, configValue });
    } catch {
      const fieldError = saveMutation.getFieldError("configValue") || saveMutation.globalError;
      if (fieldError) {
        setValueErrors((prev) => ({ ...prev, [configKey]: fieldError }));
      }
    } finally {
      setLoadingKey(null);
    }
  };

  const onToggleStatus = async (configKey: string, active: boolean) => {
    setLoadingKey(configKey);
    try {
      await toggleStatusMutation.mutateAsync({ configKey, active });
    } finally {
      setLoadingKey(null);
    }
  };

  const onReset = async (configKey: string) => {
    setLoadingKey(configKey);
    try {
      await resetMutation.mutateAsync(configKey);
    } finally {
      setLoadingKey(null);
    }
  };

  const onValueChange = (configKey: string, value: string) => {
    setValues((prev) => ({ ...prev, [configKey]: value }));
    if (valueErrors[configKey]) {
      setValueErrors((prev) => ({ ...prev, [configKey]: "" }));
    }
  };

  return (
    <AdminPoliciesScreen
      policies={policies}
      values={values}
      valueErrors={valueErrors}
      loadingKey={loadingKey}
      onValueChange={onValueChange}
      onSave={onSave}
      onToggleStatus={onToggleStatus}
      onReset={onReset}
    />
  );
}
