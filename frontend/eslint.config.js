import js from "@eslint/js";
import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";

export default tseslint.config(
  { ignores: ["dist", "node_modules", "vite.config.ts", "tailwind.config.ts"] },
  {
    extends: [js.configs.recommended, ...tseslint.configs.recommendedTypeChecked],
    files: ["src/**/*.{ts,tsx}"],
    languageOptions: {
      parserOptions: {
        project: ["./tsconfig.json", "./tsconfig.node.json"],
        tsconfigRootDir: import.meta.dirname,
      },
    },
    plugins: {
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      "react-refresh/only-export-components": "off",
      "@typescript-eslint/no-misused-promises": [
        "error",
        {
          checksVoidReturn: {
            attributes: false,
          },
        },
      ],
      "@typescript-eslint/prefer-promise-reject-errors": "off",
      "@typescript-eslint/only-throw-error": "off",
      "@typescript-eslint/no-explicit-any": "error",
    },
  },
  {
    files: ["src/pages/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["@/lib/api"],
              message: "Pages must not depend on low-level api clients. Use app/services, loaders, or feature hooks.",
              allowTypeImports: true,
            },
            {
              group: ["@/lib/api-loader", "axios"],
              message: "Pages must not call API transport directly.",
            },
          ],
        },
      ],
    },
  },
  {
    files: ["src/widgets/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["@/app/*", "@/pages/*", "@/lib/loaders/*", "@/lib/api-loader", "axios"],
              message: "Widgets are presentation layer only and must not depend on app/page/loader/transport layers.",
            },
            {
              group: ["@/router", "@/lib/api"],
              message: "Use type-only imports from router/api in widgets.",
              allowTypeImports: true,
            },
          ],
        },
      ],
    },
  },
  {
    files: ["src/app/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["@/pages/*", "@/widgets/*", "@/components/*"],
              message: "App layer must remain UI-agnostic.",
            },
          ],
        },
      ],
    },
  },
  {
    files: ["src/lib/**/*.{ts,tsx}"],
    rules: {
      "no-restricted-imports": [
        "error",
        {
          patterns: [
            {
              group: ["@/pages/*", "@/widgets/*"],
              message: "Lib layer must not depend on page/widget layers.",
            },
          ],
        },
      ],
    },
  }
);
