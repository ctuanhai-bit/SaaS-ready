# Contributing

1. Keep changes inside the hotel profile, room, inventory, booking, front-desk,
   system or infrastructure boundaries described in `docs/COMMERCIAL_BOUNDARY.md`.
2. Do not add production endpoints, credentials, certificates, customer data,
   payment implementations or private deployment scripts.
3. Run backend tests, frontend type checks, frontend build and the repository
   verification script before opening a pull request.
4. Add focused tests for inventory concurrency, order transitions and tenant or
   hotel ownership checks when those paths change.
5. Use fictional data in tests and examples.
