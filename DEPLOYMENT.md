# Belot deployment with persistent rooms

## Render + PostgreSQL

The backend now requires a PostgreSQL database. Create a PostgreSQL instance with a provider such as Neon (or Render PostgreSQL), then deploy the repository using Render Blueprint `render.yaml`.

Set these Render environment variables:

- `JDBC_DATABASE_URL` = JDBC URL, for example `jdbc:postgresql://HOST:5432/DB?sslmode=require`
- `JDBC_DATABASE_USERNAME` = database user
- `JDBC_DATABASE_PASSWORD` = database password

Use the **JDBC URL**, not the `postgresql://` URI. Never commit credentials to GitHub.

On startup, the application creates `belot_rooms` if needed and loads all stored rooms, player seat tokens, scores, hands, tricks and pending points. Each room-changing API request saves an updated serialized snapshot. Existing clients reconnect with their previously saved localStorage session tokens.

**Important:** Java-serialized room snapshots require compatible application versions. Changing serialized field types can make old snapshots unreadable; introduce versioned JSON persistence/migrations before production-scale releases. In-process rooms are not suitable for multi-instance Render scaling. Use a single Render instance for now.

Database storage is durable across app restarts provided the external PostgreSQL instance and its data remain available. This does not automatically guarantee zero data loss during database outages, crashes mid-request, or concurrent multi-instance writes.
