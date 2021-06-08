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
  plugins: [
    require('@tailwindcss/forms'),
  ],
};
