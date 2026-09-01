# Database Bootstrap

MySQL executes files in `bootstrap/` in lexical order on the first start of a
new data volume:

1. `00-framework-schema.sql` creates upstream system and infrastructure tables.
2. `01-community-seed.sql` inserts only fictional community accounts and menus.
3. `10-hotel-domain.sql` creates and seeds the hotel domain.

The framework schema is mechanically generated from the pinned upstream DDL by
`tools/generate-framework-schema.ps1`. Upstream INSERT statements are never
copied because the source baseline contains unrelated sample accounts and
third-party storage settings.

To reset local data, stop Compose and explicitly remove its volumes. This is a
destructive development operation and must never be used against shared data.
