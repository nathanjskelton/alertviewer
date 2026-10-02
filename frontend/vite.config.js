// Plugins
import vue from '@vitejs/plugin-vue'
import vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'

// Utilities
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'
import { readFileSync } from 'node:fs'

// The release version lives in ../version.env (shared with the backend). Inside the
// docker build that file is outside the context, so it arrives as the APP_VERSION build arg.
function appVersion() {
  if (process.env.APP_VERSION) return process.env.APP_VERSION
  const file = fileURLToPath(new URL('../version.env', import.meta.url))
  const match = readFileSync(file, 'utf8').match(/^APP_VERSION=(.*)$/m)
  return match ? match[1].trim() : ''
}

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue({ 
      template: { transformAssetUrls }
    }),
    // https://github.com/vuetifyjs/vuetify-loader/tree/next/packages/vite-plugin
    vuetify({
      autoImport: true,
      styles: {
        configFile: 'src/styles/settings.scss',
      },
    }),
  ],
  base: '/cortana',
  define: {
    'process.env': {},
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(appVersion()),
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    extensions: [
      '.js',
      '.json',
      '.jsx',
      '.mjs',
      '.ts',
      '.tsx',
      '.vue',
    ],
  },
  server: {
    port: 3000,
  },
})
