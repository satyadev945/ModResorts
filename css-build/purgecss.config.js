// PurgeCSS standalone configuration
// Remediates cz-css-1005 – Unused CSS in Container Images
//
// This config is used by the `npm run purge` script to produce
// intermediate purged CSS files in ./purged/ before PostCSS/cssnano
// minification runs over them.
//
// Occurrence fixed:
//   File : WebContent/styles.css  Line 13
//   Issue: CSS rules for removed components bloat the container image.
//          PurgeCSS scans all HTML/JSP/JS content and removes every
//          selector that is not referenced, reducing the final ACR image
//          size and AKS pod startup time.

module.exports = {
  // ── Input CSS files to analyse ──────────────────────────────────────────
  css: [
    { raw: require('fs').readFileSync('../WebContent/styles.css', 'utf8'),  output: 'purged/styles.css'  },
    { raw: require('fs').readFileSync('../WebContent/pikaday.css', 'utf8'), output: 'purged/pikaday.css' }
  ],

  // ── Content files whose markup determines which selectors are "used" ────
  content: [
    '../WebContent/**/*.html',
    '../WebContent/**/*.jsp',
    '../WebContent/**/*.js',
    '../src/**/*.java'
  ],

  // ── Safelist: selectors toggled dynamically from JavaScript ─────────────
  safelist: {
    standard: [
      'is-selected',
      'js-hero',
      'js-container',
      'js-city',
      'js-country',
      'js-condition',
      'js-weather-icon',
      'js-tempF',
      'js-tempC',
      'js-wind-direction',
      'js-mph',
      'js-kph',
      'js-vis-mi',
      'js-vis-km',
      'js-select'
    ],
    // Preserve all pikaday widget selectors (third-party, dynamically injected)
    greedy: [/^pika-/]
  },

  // ── Output directory for purged (pre-minification) CSS ──────────────────
  output: 'purged',

  // ── Rejected CSS audit log (written alongside purged output) ────────────
  rejected: true,
  rejectedCss: true
};
