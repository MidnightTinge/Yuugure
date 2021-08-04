module.exports = exports = () => ({
  plugins: [
    require('postcss-import'),
    require('postcss-nested'),
    require('tailwindcss')(require('./tailwind.config')),
    require('autoprefixer'),
    require('postcss-csso'),
  ]
});
