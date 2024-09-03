import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching';
import { registerRoute } from 'workbox-routing';
import { CacheFirst, StaleWhileRevalidate } from 'workbox-strategies';
import { ExpirationPlugin } from 'workbox-expiration';
import { clientsClaim, skipWaiting } from 'workbox-core';

// Automatically cleanup outdated caches
cleanupOutdatedCaches();

// Claim clients immediately after the service worker becomes active
clientsClaim();

// Skip waiting and activate the new service worker immediately
skipWaiting();

// Precache and route assets
precacheAndRoute(self.__WB_MANIFEST || []);  // __WB_MANIFEST will be populated by Workbox

// Runtime caching for scripts and styles
registerRoute(
  ({ request }) => request.destination === 'script' || request.destination === 'style',
  new StaleWhileRevalidate({
    cacheName: 'projectName-runtime-cache',
    plugins: [
      new ExpirationPlugin({
        maxEntries: 50,
        maxAgeSeconds: 30 * 24 * 60 * 60, // 30 days
      }),
    ],
  })
);

// Cache images with CacheFirst strategy
registerRoute(
  ({ request }) => request.destination === 'image',
  new CacheFirst({
    cacheName: 'projectName-images-cache',
    plugins: [
      new ExpirationPlugin({
        maxEntries: 50,
        maxAgeSeconds: 30 * 24 * 60 * 60, // 30 days
      }),
    ],
  })
);

// Do not cache API requests - by not registering a route for them

// Security best practices: This is often handled by server headers
// Ensure your server is configured to set the following headers:
// Content-Security-Policy: script-src 'self';
// X-Content-Type-Options: nosniff;
// X-Frame-Options: SAMEORIGIN;
// X-XSS-Protection: 1; mode=block;
