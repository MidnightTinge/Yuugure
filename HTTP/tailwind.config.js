const defaultConfig = require('tailwindcss/defaultConfig');
const plugin = require('tailwindcss/plugin');

const sizingPoints = {
  '1/2': '50%',
  '1/3': '33.333333%',
  '2/3': '66.666667%',
  '1/4': '25%',
  '2/4': '50%',
  '3/4': '75%',
  '1/5': '20%',
  '2/5': '40%',
  '3/5': '60%',
  '4/5': '80%',
  '1/6': '16.666667%',
  '2/6': '33.333333%',
  '3/6': '50%',
  '4/6': '66.666667%',
  '5/6': '83.333333%',
  '1/12': '8.333333%',
  '2/12': '16.666667%',
  '3/12': '25%',
  '4/12': '33.333333%',
  '5/12': '41.666667%',
  '6/12': '50%',
  '7/12': '58.333333%',
  '8/12': '66.666667%',
  '9/12': '75%',
  '10/12': '83.333333%',
  '11/12': '91.666667%',
  auto: 'auto',
  full: '100%',
  screen: '100vw',
  min: 'min-content',
  max: 'max-content',
  none: 'none',
};

module.exports = {
  purge: [
    './src/views/**/*.pebble',
    './src/css/**/*.css',
    './src/js/**/*.tsx',
    './src/js/**/*.ts',
  ],
  darkMode: false,
  variants: {
    extend: {
      cursor: ['responsive', 'disabled'],
      opacity: ['responsive', 'disabled', 'group-hover', 'focus-within', 'hover', 'focus'],
      backgroundColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
      borderColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
      borderWidth: ['responsive', 'hover', 'group-hover', 'focus', 'disabled'],
      textColor: ['responsive', 'disabled', 'dark', 'group-hover', 'focus-within', 'hover', 'focus'],
    },
  },
  theme: {
    // make the min/max sizing utilities match their standalone utilities (adds things like min-w-96)
    minWidth: {
      ...defaultConfig.theme.minWidth,
      ...defaultConfig.theme.spacing,
      ...sizingPoints,
    },
    maxWidth: {
      ...defaultConfig.theme.maxWidth,
      ...defaultConfig.theme.spacing,
      ...sizingPoints,
    },
    minHeight: {
      ...defaultConfig.theme.minHeight,
      ...defaultConfig.theme.spacing,
      ...sizingPoints,
    },
    maxHeight: {
      ...defaultConfig.theme.maxHeight,
      ...defaultConfig.theme.spacing,
      ...sizingPoints,
      'full-no-nav': 'calc(100% - 2.5em)',
      'screen-no-nav': 'calc(100vh - 2.5em)',
    },
  },
  plugins: [
    require('@tailwindcss/forms'),

    // Reset the maxWidth and height constraints set by the tailwind preflight. This allows us to
    // handle image aspect-ratio resizing on our own. Given the individualized nature of the app,
    // we need direct control over these things.
    plugin(({addBase}) =>
      addBase({
        'img': {maxWidth: 'none', height: 'unset'},
        'video': {maxWidth: 'none', height: 'unset'},
      }),
    ),
  ],
};
