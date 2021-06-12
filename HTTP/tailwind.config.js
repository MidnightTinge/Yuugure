const defaultConfig = require('tailwindcss/defaultConfig');

module.exports = {
  purge: [
    './src/views/**/*.pebble',
    './src/js/**/*.tsx',
  ],
  darkMode: false,
  variants: {
    extend: {
      cursor: ['responsive', 'disabled'],
      opacity: ['responsive', 'disabled', 'group-hover', 'focus-within', 'hover', 'focus'],
      backgroundColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
      borderColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
      textColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
    },
  },
  theme: {
    // make the min/max sizing utilities match their standalone utilities (adds things like min-w-96)
    minWidth: {
      ...defaultConfig.theme.minWidth,
      ...defaultConfig.theme.spacing,
    },
    maxWidth: {
      ...defaultConfig.theme.maxWidth,
      ...defaultConfig.theme.spacing,
    },
    minHeight: {
      ...defaultConfig.theme.minHeight,
      ...defaultConfig.theme.spacing,
    },
    maxHeight: {
      ...defaultConfig.theme.maxHeight,
      ...defaultConfig.theme.spacing,
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
};
