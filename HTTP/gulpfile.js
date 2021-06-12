const gulp = require('gulp');

const inRoot = './src';
const outRoot = '../src/main/resources';

function views() {
  return gulp.src(`${inRoot}/views/**/*.pebble`)
    .pipe(gulp.dest(`${outRoot}/templates/`));
}

function css() {
  const sass = require('gulp-sass');
  const maps = require('gulp-sourcemaps');
  sass.compiler = require('node-sass');

  return gulp.src(`${inRoot}/css/**/*.css`) // TODO need to figure out what exact files to include, don't want to compile a bunch of utility classes into separate files.
    .pipe(maps.init())
    .pipe(sass().on('error', sass.logError))
    .pipe(require('gulp-postcss')([
      require('postcss-import'),
      require('postcss-nested'),
      require('tailwindcss')(require('./tailwind.config')),
      require('autoprefixer'),
      require('postcss-csso'),
    ]))
    .pipe(maps.write('./')) // we want to keep maps separate as they're bulking filesizes up to 1mb+ on larger files
    .pipe(gulp.dest(`${outRoot}/static/css/`));
}

function frags() {
  return gulp.src(`${inRoot}/vend/frags/**/*`)
    .pipe(gulp.dest(`${outRoot}/static/`));
}

function js() {
  return gulp.src(`${inRoot}/js/**/*.*`)
    .pipe(require('webpack-stream')({
      mode: process.env.NODE_ENV === 'development' ? 'development' : 'production',
      entry: {
        app: `${inRoot}/js/app.tsx`,
      },
      output: {
        filename: 'js/[name].bundle.js',
        path: require('path').resolve(outRoot),
      },
      devtool: 'nosources-source-map',
      module: {
        rules: [
          {
            test: /\.tsx?$/,
            exclude: /(node_modules)/,
            resolve: {
              extensions: ['.tsx', '.ts', '.jsx', '.js', '.json'],
            },
            use: [
              {
                loader: 'babel-loader',
                options: {
                  presets: [
                    '@babel/preset-env',
                    '@babel/preset-typescript',
                    '@babel/preset-react',
                  ],
                  plugins: [
                    ['@babel/plugin-transform-runtime', {regenerator: true}],
                    ['@babel/plugin-proposal-class-properties'],
                  ],
                },
              },
            ],
          },
          {
            test: /\.m?js$/,
            exclude: /(node_modules)/,
            use: [
              {
                loader: 'babel-loader',
                options: {
                  presets: ['@babel/preset-env'],
                  plugins: [
                    [
                      '@babel/plugin-transform-runtime',
                      {regenerator: true},
                    ],
                  ],
                },
              },
              'webpack-conditional-loader',
            ],
          },
        ],
      },
    }))
    .pipe(gulp.dest(`${outRoot}/static/`));
}

function watch(cb) {
  gulp.watch([`${inRoot}/js/**/*.ts`, `${inRoot}/js/**/*.tsx`, `${inRoot}/js/**/*.js`, `${inRoot}/js/**/*.jsx`], gulp.series(js));
  gulp.watch(`${inRoot}/css/**/*.css`, gulp.series(css));
  gulp.watch(`${inRoot}/vend/frags/**/*`, gulp.series(frags));
  gulp.watch(`${inRoot}/views/**/*`, gulp.series(views));
  cb();
}

function defaultTask(cb) {
  console.error('no default task defined');
  cb();
}

exports.views = views;
exports.js = js;
exports.css = css;
exports.frags = frags;
exports.watch = watch;
exports.build = gulp.parallel(views, css, frags, js);
exports.default = defaultTask;
