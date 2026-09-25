// PostCSS configuration for CSS minification and unused-rule removal
// Remediates:
//   cz-css-1004 – Unminified CSS in Production Containers (cssnano)
//   cz-css-1005 – Unused CSS in Container Images (postcss-purgecss)
//
// PurgeCSS (cz-css-1005):
//   Scans all HTML, JSP, and JS content files to detect which CSS selectors
//   are actually referenced, then strips every unused rule from styles.css
//   before the image is assembled.  This directly addresses the bloat caused
//   by CSS rules for removed components (styles.css line 13 and beyond).
//
// cssnano (cz-css-1004):
//   Applied after PurgeCSS so only the surviving rules are minified:
//   - Remove all comments (discardComments)
//   - Collapse whitespace (normalizeWhitespace)
//   - Merge duplicate/redundant selectors (mergeRules)
//   - Merge longhand properties into shorthand (mergeLonghand)
//   - Minify font-value declarations (minifyFontValues)
//   - Minify @media / @keyframes params (minifyParams)
//   - Optimise selector specificity (minifySelectors)
//
// reduceIdents is disabled to avoid renaming @keyframes / CSS custom properties
// that may be referenced from JavaScript.
module.exports = {
  plugins: [
    // ── Step 1: Remove unused CSS rules (cz-css-1005) ──────────────────────
    require('@fullhuman/postcss-purgecss')({
      // Content files whose class/id references determine which CSS rules survive
      content: [
        '../WebContent/**/*.html',
        '../WebContent/**/*.jsp',
        '../WebContent/**/*.js',
        '../src/**/*.java'
      ],
      // Safelist selectors that are added dynamically from JavaScript
      // (e.g. 'is-selected' toggled via classList in main.js)
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
        // Preserve all pikaday-prefixed selectors (third-party widget)
        greedy: [/^pika-/]
      },
      // Treat each CSS declaration block as a separate unit for removal
      rejected: false,
      // Emit a list of removed selectors to stdout for audit purposes
      rejectedCss: false
    }),

    // ── Step 2: Minify surviving rules (cz-css-1004) ───────────────────────
    require('cssnano')({
      preset: [
        'default',
        {
          discardComments:     { removeAll: true },
          normalizeWhitespace: true,
          minifySelectors:     true,
          minifyFontValues:    true,
          minifyParams:        true,
          reduceIdents:        false,
          mergeRules:          true,
          mergeLonghand:       true
        }
      ]
    })
  ]
};
