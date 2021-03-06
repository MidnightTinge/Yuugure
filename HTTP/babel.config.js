module.exports = exports = {
  presets: [
    '@babel/preset-env',
    '@babel/preset-typescript',
    '@babel/preset-react',
  ],
  plugins: [
    ['@babel/plugin-transform-runtime', {regenerator: true}],
    ['@babel/plugin-proposal-class-properties'],
  ],
};
