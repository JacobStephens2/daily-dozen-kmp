// sql.js loads its WebAssembly engine (sql-wasm.wasm) at runtime by fetching it
// next to the worker. Copy that file into the webpack output so the browser can
// find it. Files in webpack.config.d/ are merged into the generated config.
const CopyWebpackPlugin = require('copy-webpack-plugin');

config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm'
        ]
    })
);

// sql.js ships a Node build path that references core modules; the browser
// build doesn't need them, so stub them out rather than polyfill.
config.resolve = config.resolve || {};
config.resolve.fallback = Object.assign({}, config.resolve.fallback, {
    fs: false,
    path: false,
    crypto: false,
});
