const CACHE_NAME = 'my-pwa-cache-v1';
const PRECACHE_URLS = [
  '/ccca/care/',              // Cache the root URL
  '/ccca/care/index.html',    // Cache the main HTML file
];

// Install event: Cache initial resources
self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then((cache) => {
        console.log('Opened cache');
        return cache.addAll(PRECACHE_URLS); // Cache specific URLs
      })
  );
  self.skipWaiting(); // Activate the new service worker immediately
});

// Activate event: Clean up old caches
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName); // Delete old caches
          }
        })
      );
    })
  );
  self.clients.claim(); // Claim control of all clients as soon as the service worker is activated
});

// Fetch event: Serve from cache or fetch from network
self.addEventListener('fetch', (event) => {
  const requestUrl = new URL(event.request.url);

  // Cache first for JS, CSS, and image files
  if (/\/ccca\/care\/.*(\.js$|\.css$|\.png$|\.jpg$|\.jpeg$|\.svg$)/.test(requestUrl.pathname)) {
    event.respondWith(
      caches.match(event.request)
        .then((response) => {
          if (response) {
            return response; // Cache hit - return the response from the cache
          }

          // Cache miss - fetch from network and cache the response
          return fetch(event.request).then((networkResponse) => {
            if (!networkResponse || networkResponse.status !== 200 || networkResponse.type !== 'basic') {
              return networkResponse;
            }

            // Clone the response for caching
            const responseToCache = networkResponse.clone();

            caches.open(CACHE_NAME)
              .then((cache) => {
                cache.put(event.request, responseToCache);
              });

            return networkResponse;
          });
        })
    );
  } else {
    // Network first for other requests (like API calls)
    event.respondWith(
      fetch(event.request).catch(() => caches.match(event.request))
    );
  }
});
