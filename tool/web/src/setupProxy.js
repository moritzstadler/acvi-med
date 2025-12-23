const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/cspec',
    createProxyMiddleware({
      target: 'https://cspec.genome.network',
      changeOrigin: true,
      secure: true,
    })
  );
};

