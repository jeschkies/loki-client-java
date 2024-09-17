# Loki Client Java
Loki Java client that sends and retrieves logs to and from a running Loki server 

## Release Steps

1. Update the version numbers in `loki-client/build.gradle.kts` and `loki-client-testutils/build.gradle.kts`. Not that there are multiple lines to change.
2. Land the update on `main`.
3. Update your local main and tag the release commit `git tag vX.Y.Z --sign -u <key id>`.
4. Push the tag `git push -f origin tag vX.Z.Z`.