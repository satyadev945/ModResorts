// =============================================================================
// PurgeCSS Configuration
// Remediation for: cz-css-1005 (Unused CSS in Container Images)
//
// Violation: styles.css Line 13 — CSS file contains rules for removed/unused
// components that bloat the container image, increasing size and deployment time.
//
// Strategy: Google Cloud Build with PurgeCSS and Artifact Registry for GKE
// Autopilot Deployments. PurgeCSS scans all HTML, JSP, and JS content files
// to determine which CSS selectors are actually referenced, then removes all
// unused rules from styles.css before the production image is built.
//
// This configuration is consumed by:
//   - `npm run purge-css` (local development)
//   - `npm run optimize-css` (purge + minify pipeline)
//   - cloudbuild.yaml Step 1 (Google Cloud Build CI/CD)
//   - Dockerfile Stage 1 (multi-stage container build)
// =============================================================================

/** @type {import('purgecss').UserDefinedOptions} */
module.exports = {
  // Content files to scan for used CSS selectors.
  // PurgeCSS parses these files and retains only the CSS rules whose
  // selectors appear in at least one of these content sources.
  content: [
    // HTML entry point and JSP login page
    "WebContent/**/*.html",
    "WebContent/**/*.jsp",
    // JavaScript files that dynamically add/remove CSS classes at runtime
    "WebContent/**/*.js",
    // Java source — servlet code may emit class names in JSON/HTML responses
    "src/**/*.java"
  ],

  // CSS files to purge.
  // styles.css (line 13) is the primary target identified by cz-css-1005.
  // pikaday.css is included to remove any unused date-picker rules as well.
  css: [
    "WebContent/styles.css",
    "WebContent/pikaday.css"
  ],

  // Output directory for purged CSS files.
  // The Dockerfile and cloudbuild.yaml copy these optimized files into the
  // production image, replacing the originals.
  output: "dist/",

  // Safelist: selectors that are added dynamically at runtime (e.g., via
  // JavaScript classList.add / setAttribute) and would otherwise be removed
  // by static analysis. These classes MUST be preserved in the output CSS.
  safelist: {
    // Standard patterns — exact class names always kept
    standard: [
      // Dynamically toggled by main.js to reveal the destination details panel
      "is-selected",
      // Weather icon classes set programmatically based on API response
      "weather-icon",
      // Pikaday date-picker classes injected into the DOM by pikaday.js
      /^pika-/,
      /^is-/
    ],
    // Deep patterns — keep any selector that matches these regexes even if
    // not found literally in the scanned content files
    deep: [
      // Preserve all pseudo-element rules (e.g., .line:after) since PurgeCSS
      // may not detect pseudo-selectors used only in CSS cascade logic
      /::?after$/,
      /::?before$/
    ],
    // Greedy patterns — keep entire rule blocks whose selector matches
    greedy: [
      // Retain all js- prefixed classes used as JavaScript hooks in main.js
      /^js-/
    ]
  },

  // Rejected CSS: write a separate file listing all removed selectors.
  // Useful for auditing which rules were stripped from the production image.
  rejected: true,
  rejectedCss: true,

  // fontFace: remove unused @font-face declarations (fonts not referenced
  // in any retained CSS rule are stripped from the output).
  fontFace: true,

  // keyframes: remove unused @keyframes animation blocks.
  keyframes: true,

  // variables: remove unused CSS custom properties (--var declarations).
  variables: true
};
