/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL: string;
  readonly VITE_MOCK_UPLOAD_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

// Build-time feature flags
declare const __FEATURE_COMMUNITY__: boolean;
declare const __FEATURE_SEARCH__: boolean;
declare const __FEATURE_PROFILE_EDIT__: boolean;
declare const __FEATURE_EVENTS__: boolean;
declare const __FEATURE_INSTAGRAM__: boolean;
declare const __FEATURE_DARK_MODE__: boolean;
