# Device-test signing backup

`device-test.keystore.enc` is an encrypted backup of the existing Android debug
signing identity, retained with owner authorization. It is not a production key.
The passphrase is stored separately as GitHub Actions secret
`DEVICE_TEST_BACKUP_PASSPHRASE`; never print or commit its value.

Restore in a private temporary directory with `umask 077`:

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 \
  -in signing/device-test.keystore.enc -out device-test.keystore \
  -pass file:/path/to/private/passphrase-file
```

Verify certificate SHA-256 before using:
`9ddb7f477828847e3908328c4cfea0bb9d5c50124a41743dd42c2443abd83d03`.
The existing DEVICE_TEST_KEYSTORE_BASE64 secret remains available for CI signing.
