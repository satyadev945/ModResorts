/**
 * PostCSS Configuration for CSS Minification
 * Remediation: cr-css-1005 - CSS Files Not Minified for Production
 *
 * Uses CSSNano to minify CSS files for AWS CodePipeline build stage.
 * Minified artifacts are deployed to S3 and served via CloudFront
 * for reduced bandwidth and faster CDN delivery.
 */
module.exports = {
  plugins: [
    require('cssnano')({
      preset: [
        'default',
        {
          // Remove all comments (including license comments in non-prod)
          discardComments: {
            removeAll: false,        // Keep /*! ... */ license comments
            removeAllButFirst: true  // Keep only the first license comment
          },
          // Normalise whitespace
          normalizeWhitespace: true,
          // Merge duplicate rules
          mergeLonghand: true,
          mergeRules: true,
          // Minify selectors
          minifySelectors: true,
          // Minify font values
          minifyFontValues: true,
          // Minify gradients
          minifyGradients: true,
          // Minify parameters
          minifyParams: true,
          // Reduce initial values
          reduceInitial: true,
          // Reduce transforms
          reduceTransforms: true,
          // Normalise url()
          normalizeUrl: true,
          // Convert colour values to shortest form
          colormin: true,
          // Convert values to shorter equivalents
          convertValues: true,
          // Calc optimisation
          calc: true,
          // Ordered values
          orderedValues: true,
          // Unique selectors
          uniqueSelectors: true,
          // CSS source maps disabled for production artifacts
          cssDeclarationSorter: false
        }
      ]
    })
  ]
};
