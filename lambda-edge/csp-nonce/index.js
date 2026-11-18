'use strict';
// Lambda@Edge (origin-response) handler: generates a per-response CSP nonce,
// replaces the __CSP_NONCE__ placeholder in HTML bodies and sets the CSP header.
// Associate with the CloudFront distribution as an "Origin response" trigger
// with "Include body" enabled.
const crypto = require('crypto');

exports.handler = async (event) => {
  const response = event.Records[0].cf.response;
  const headers = response.headers || {};
  const ct = headers['content-type'] && headers['content-type'][0].value;
  if (!ct || ct.indexOf('text/html') !== 0) {
    return response;
  }
  const nonce = crypto.randomBytes(16).toString('base64');
  if (response.body && response.body.data !== undefined) {
    const html = Buffer.from(response.body.data, response.body.encoding === 'base64' ? 'base64' : 'utf8').toString('utf8');
    response.body.data = Buffer.from(html.split('__CSP_NONCE__').join(nonce), 'utf8').toString('base64');
    response.body.encoding = 'base64';
  }
  headers['content-security-policy'] = [{
    key: 'Content-Security-Policy',
    value: "default-src 'self'; script-src 'self' 'nonce-" + nonce + "'; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; img-src 'self' data: https://icons.wxug.com; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'self'"
  }];
  delete headers['content-length'];
  response.headers = headers;
  return response;
};
