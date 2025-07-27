module.exports = {
    parser: '@typescript-eslint/parser',
    parserOptions: {
        ecmaVersion: 2020,
        sourceType: 'module'
    },
    extends: ['plugin:@typescript-eslint-recommended', 'prettier/@typescript-eslint', 'plugin:prettier/recommended'],
    rules: {
        // we can override/disable rules below, e.g; with "@typescript-eslint/explicit-function-return-type": "off"
        'sort-imports': ['warn']
    }
}