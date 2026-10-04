# API contract

`openapi.json` is the generated OpenAPI 3.1 description of every route this backend exposes.
It is committed so that changing a route shows up in a code review as a diff, instead of
being invisible in a controller method signature.

## Regenerating

Run this from the `backend` directory after changing any controller, request mapping,
entity field, or `SecurityConfig` matcher:

```powershell
.\mvnw.cmd -B test "-Dtest=OpenApiContractTest"
```

Commit the regenerated `openapi.json` in the same commit as the code change. If the test
passes but the file is not updated, the change did not affect the contract.

## What the test also enforces

`OpenApiContractTest` does more than write the file. It fails the build when:

- a route takes the owner from the path or a `userId` query parameter, which would let any
  caller act on any account;
- a route removed during the security work reappears;
- a resource is added outside the `/api` prefix without being added to the migration list;
- a credential endpoint is documented as requiring a token it cannot have yet;
- a seller route takes the product or the owner from the body rather than the path and the token;
- a schema exposes a lazy collection or a server-owned field such as `VendorProfile.verified`.

`KNOWN_UNFIXED_IDOR_ROUTES` is empty: every owner in the API is derived from the caller's token, so
no route takes a `userId` to decide what a caller may read or change. If a future change needs to
list a vulnerable route there, that is a bug being reintroduced rather than a to-do being tracked.

## Viewing it

With the backend running, Swagger UI is at `/swagger-ui.html`.