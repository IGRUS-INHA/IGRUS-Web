/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Build-time feature flags
declare const __FEATURE_COMMUNITY__: boolean;
declare const __FEATURE_SEARCH__: boolean;
declare const __FEATURE_PROFILE_EDIT__: boolean;
