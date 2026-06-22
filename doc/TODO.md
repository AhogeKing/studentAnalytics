# TODO

This document tracks the next practical backend tasks for the current `studentAnalytics` codebase.

## Authentication

- Add a real logout invalidation strategy if the project needs server-side token revocation. The current JWT flow is stateless, so logout should still require the frontend to remove the token. A future implementation can use a token blacklist backed by Redis or a database table.
- Add an authenticated current-user endpoint, for example `GET /me`, using `SysUserContext.getUserId()` and `SysUserMapper`.
- Decide whether failed authentication should keep returning HTTP 200 with `Result.error(...)`, or whether the API should use HTTP 401 plus the same JSON body.

## User Management

- Add role validation for registration. Current accepted roles should align with `sys_user.role`: `STUDENT`, `TEACHER`, and `ADMIN`.
- Add a default role policy. Public registration probably should not allow arbitrary `ADMIN` creation.
- Add duplicate username handling based on database unique-key exceptions as a fallback in addition to the current pre-check.

## API Quality

- Add request examples for Bruno covering register, duplicate register, login success, login failure, logout without token, and logout with token.
- Add integration tests around `SysUserController` once the local database test strategy is settled.
- Add a global fallback exception handler for unexpected exceptions, while keeping sensitive error details out of responses.

## Configuration

- Keep `src/main/resources/jwt.yml` and `python/importer/.env` local-only. Do not commit real JWT secrets or database credentials.
- Consider adding a safe template file for backend runtime config if more local-only settings are needed.
- Review whether importing `python/importer/.env` from the Java app should remain long term, or whether backend config should move to a dedicated backend-local env file.

## Data Model

- Add entity classes and mappers for the remaining V0 tables when their endpoints are planned.
- Add field-level validation for DTOs that map to constrained SQL fields, especially role/status and dataset-coded enum values.
