# using a modded s3 mock container because the original serves HTTPS on port 9191, and we want it on 443
FROM adobe/s3mock:3.1.0

ENV server.port=443
