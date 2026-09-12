/**
 * CSS Minification Script for ModResorts
 * Remediation: cr-css-1005 - CSS Files Not Minified for Production
 *
 * Scans WebContent directory for non-minified CSS files and produces
 * minified output in dist/css/ for deployment to AWS S3 + CloudFront.
 *
 * Usage:
 *   node scripts/minify-css.js
 *
 * Environment variables:
 *   CSS_SOURCE_DIR  - Source directory (default: WebContent)
 *   CSS_OUTPUT_DIR  - Output directory (default: dist/css)
 */

'use strict';

const fs   = require('fs');
const path = require('path');

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------
const SOURCE_DIR = process.env.CSS_SOURCE_DIR || 'WebContent';
const OUTPUT_DIR = process.env.CSS_OUTPUT_DIR || 'dist/css';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

/**
 * Recursively collect all *.css files that are NOT already *.min.css.
 * @param {string} dir
 * @returns {string[]}
 */
function collectCssFiles(dir) {
  const results = [];
  if (!fs.existsSync(dir)) {
    console.warn(`[minify-css] Source directory not found: ${dir}`);
    return results;
  }
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      results.push(...collectCssFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith('.css') && !entry.name.endsWith('.min.css')) {
      results.push(fullPath);
    }
  }
  return results;
}

/**
 * Ensure all parent directories for a file path exist.
 * @param {string} filePath
 */
function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

/**
 * Apply CSSNano minification via PostCSS.
 * Falls back to a simple whitespace-strip if PostCSS is unavailable.
 *
 * @param {string} css   - Raw CSS source
 * @param {string} from  - Source file path (for source-map metadata)
 * @returns {Promise<string>} - Minified CSS string
 */
async function minifyCss(css, from) {
  try {
    const postcss = require('postcss');
    const cssnano  = require('cssnano');
    const result   = await postcss([
      cssnano({
        preset: ['default', {
          discardComments:    { removeAllButFirst: true },
          normalizeWhitespace: true,
          mergeLonghand:       true,
          mergeRules:          true,
          minifySelectors:     true,
          minifyFontValues:    true,
          minifyGradients:     true,
          minifyParams:        true,
          reduceInitial:       true,
          colormin:            true,
          convertValues:       true,
          uniqueSelectors:     true
        }]
      })
    ]).process(css, { from, map: false });
    return result.css;
  } catch (err) {
    // Graceful fallback: strip comments and collapse whitespace
    console.warn(`[minify-css] PostCSS/CSSNano unavailable (${err.message}), using fallback minifier.`);
    return css
      .replace(/\/\*(?!!)[^*]*\*+([^/*][^*]*\*+)*\//g, '') // strip non-license comments
      .replace(/\s*([{}:;,>~+])\s*/g, '$1')                 // remove spaces around punctuation
      .replace(/\s+/g, ' ')                                  // collapse whitespace
      .trim();
  }
}

// ---------------------------------------------------------------------------
// Main
// ---------------------------------------------------------------------------
(async () => {
  const cssFiles = collectCssFiles(SOURCE_DIR);

  if (cssFiles.length === 0) {
    console.log('[minify-css] No CSS files found to minify.');
    process.exit(0);
  }

  console.log(`[minify-css] Found ${cssFiles.length} CSS file(s) to minify.`);

  let successCount = 0;
  let failCount    = 0;

  for (const srcFile of cssFiles) {
    const relativePath = path.relative(SOURCE_DIR, srcFile);
    const baseName     = path.basename(srcFile, '.css');
    const outFile      = path.join(OUTPUT_DIR, path.dirname(relativePath), `${baseName}.min.css`);

    try {
      const raw       = fs.readFileSync(srcFile, 'utf8');
      const minified  = await minifyCss(raw, srcFile);

      ensureDir(outFile);
      fs.writeFileSync(outFile, minified, 'utf8');

      const origSize = Buffer.byteLength(raw,      'utf8');
      const minSize  = Buffer.byteLength(minified, 'utf8');
      const saving   = origSize > 0 ? (((origSize - minSize) / origSize) * 100).toFixed(1) : '0.0';

      console.log(`[minify-css] ✓ ${srcFile} → ${outFile}  (${origSize}B → ${minSize}B, ${saving}% saved)`);
      successCount++;
    } catch (err) {
      console.error(`[minify-css] ✗ Failed to minify ${srcFile}: ${err.message}`);
      failCount++;
    }
  }

  console.log(`\n[minify-css] Done. ${successCount} succeeded, ${failCount} failed.`);
  process.exit(failCount > 0 ? 1 : 0);
})();
