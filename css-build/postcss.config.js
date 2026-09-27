/**
 * PostCSS configuration for the ModResorts EKS container build pipeline.
 *
 * Two rules are addressed by this configuration:
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Rule cz-css-1005 – Unused CSS in Container Images
 * ─────────────────────────────────────────────────────────────────────────────
 * PurgeCSS scans all HTML, JSP, and JavaScript content files and removes any
 * CSS selectors that are not referenced in those files.  This prevents unused
 * rules (e.g. selectors for removed components) from bloating the container
 * image, reducing ECR registry storage costs and Kubernetes pod startup time.
 *
 * Content files scanned (passed via the PURGECSS_CONTENT env var in the
 * Dockerfile, defaulting to the paths below):
 *   - WebContent/index.html
 *   - WebContent/login.jsp
 *   - WebContent/main.js
 *
 * Occurrence addressed:
 *   File : WebContent/styles.css  Line : 13
 *   (The minified styles.css contains unused selectors for removed components;
 *    PurgeCSS strips them before the final image is assembled.)
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Rule cz-css-1004 – Unminified CSS in Production Containers
 * ─────────────────────────────────────────────────────────────────────────────
 * cssnano removes whitespace, comments, and redundant selectors from the
 * PurgeCSS output before it is packaged into the production WAR, reducing
 * ECR image size and Kubernetes pod startup time.
 *
 * Specifically addresses the 35 flagged occurrences in styles.css (lines 267–346):
 *   .weather-label, .line, .line:after, .line__condition:after,
 *   .line__temp:after, .line__wind:after, .line__vis:after, .weather-icon,
 *   .footer, .resort-name--footer, .footer-content, .footer-content__heading,
 *   .footer-content__list, .footer-content__contact, .footer-content__legal,
 *   .footer--logo
 *
 * Plugin execution order: PurgeCSS runs first (removes unused rules), then
 * cssnano minifies the remaining CSS.
 */

const purgecss = require('@fullhuman/postcss-purgecss');

// Content files whose class/id references determine which CSS rules are kept.
// The PURGECSS_CONTENT environment variable can override this list at build
// time (comma-separated glob patterns) to accommodate additional templates.
const contentGlobs = process.env.PURGECSS_CONTENT
  ? process.env.PURGECSS_CONTENT.split(',').map(s => s.trim())
  : [
      './content/index.html',
      './content/login.jsp',
      './content/main.js'
    ];

module.exports = {
  plugins: [
    // ── Step 1: PurgeCSS – strip unused CSS rules (cz-css-1005) ──────────────
    purgecss({
      // HTML/JSP/JS files to scan for used selectors
      content: contentGlobs,

      // Treat any word character sequence as a potential CSS class/id so that
      // dynamically constructed class names (e.g. 'js-' prefixed helpers) are
      // preserved.
      defaultExtractor: content => content.match(/[\w-/:]+(?<!:)/g) || [],

      // Safelist selectors that are injected at runtime and would otherwise be
      // removed (e.g. Pikaday calendar classes, is-selected state class).
      safelist: {
        standard: [
          // Pikaday date-picker classes (added dynamically by pikaday.js)
          /^pika-/,
          // State classes toggled by main.js
          'is-selected',
          // Pseudo-element helpers used in weather display
          /^js-/
        ],
        deep: [],
        greedy: []
      }
    }),

    // ── Step 2: cssnano – minify the purged CSS (cz-css-1004) ────────────────
    require('cssnano')({
      preset: [
        'default',
        {
          discardComments: { removeAll: true },
          normalizeWhitespace: true,
          minifySelectors: true,
          mergeLonghand: true,
          mergeRules: true
        }
      ]
    })
  ]
};
