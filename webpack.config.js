const HtmlWebpackPlugin = require('html-webpack-plugin');
const path = require('path');
const webpack = require('webpack');

module.exports = {
  context: __dirname,
  devtool: 'source-map',
  entry: {
    main: './src/entry'
  },
  output: {
    path: __dirname,
    filename: 'build.js'
  },
  module: {
    rules: [
      {
        test: /\.kt$/,
        use: [
          {
            loader: 'webpack-kotlin-loader',
            options: {
              srcRoot: path.resolve(__dirname, './src')
            }
          }
        ]
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './src/index.html',
      hash: true
    })
  ]
};
