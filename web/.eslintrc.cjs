module.exports = {
  root: true,
  env: { browser: true, es2020: true },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
  ],
  ignorePatterns: ['dist', '.eslintrc.cjs'],
  parser: '@typescript-eslint/parser',
  plugins: ['react-refresh', 'react-compiler'],
  rules: {
    'react-refresh/only-export-components': [
      'warn',
      { allowConstantExport: true },
    ],
    'react-compiler/react-compiler': 'error',
    'no-restricted-imports': [
      'warn',
      {
        paths: [
          {
            name: 'react',
            importNames: ['useMemo', 'useCallback'],
            message: 'React Compiler가 메모이제이션을 자동 처리합니다. 서드파티 라이브러리 연동 등 예외적으로 필요한 경우 eslint-disable 주석으로 허용하세요.',
          },
        ],
      },
    ],
    'react-hooks/exhaustive-deps': 'off',
  },
}
