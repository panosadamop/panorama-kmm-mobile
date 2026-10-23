"""One-time (and yearly, on certificate expiry) CI signing setup — no Mac needed.

Creates an Apple Distribution certificate through the App Store Connect API,
packs it with a freshly generated private key into a .p12, and stores everything
the `ios-testflight` workflow needs as GitHub repo secrets.

    pip install cryptography pyjwt requests
    python iosApp/fastlane/setup_signing.py <AuthKey_XXXX.p8> <key id> <issuer id> <backup dir>

The .p12 and its password are also written to <backup dir>: keep them (password
manager / offline copy). Apple allows only a few distribution certificates, so
re-running this creates another one rather than reusing the old one.
"""
import base64
import pathlib
import secrets
import subprocess
import sys
import time

import jwt
import requests
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.primitives.serialization import pkcs12
from cryptography.x509.oid import NameOID

REPO = "panosadamop/panorama-kmm-mobile"
ROOT = pathlib.Path(__file__).resolve().parents[2]
PLIST = ROOT / "iosApp" / "iosApp" / "GoogleService-Info.plist"


def token(p8: str, key_id: str, issuer: str) -> str:
    now = int(time.time())
    return jwt.encode(
        {"iss": issuer, "iat": now, "exp": now + 15 * 60, "aud": "appstoreconnect-v1"},
        p8, algorithm="ES256", headers={"kid": key_id, "typ": "JWT"},
    )


def set_secret(name: str, value: str) -> None:
    subprocess.run(["gh", "secret", "set", name, "--repo", REPO], input=value.encode(), check=True)
    print(f"  secret {name} set")


def main(p8_path: str, key_id: str, issuer: str, backup_dir: str) -> None:
    p8 = pathlib.Path(p8_path).read_text()
    backup = pathlib.Path(backup_dir)
    backup.mkdir(parents=True, exist_ok=True)
    if not PLIST.exists():
        sys.exit(f"missing {PLIST}")

    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    csr = (
        x509.CertificateSigningRequestBuilder()
        .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, "Panorama CI")]))
        .sign(key, hashes.SHA256())
    )

    resp = requests.post(
        "https://api.appstoreconnect.apple.com/v1/certificates",
        headers={"Authorization": f"Bearer {token(p8, key_id, issuer)}"},
        json={"data": {"type": "certificates", "attributes": {
            "certificateType": "DISTRIBUTION",
            "csrContent": csr.public_bytes(serialization.Encoding.PEM).decode(),
        }}},
        timeout=60,
    )
    if resp.status_code != 201:
        sys.exit(f"Apple refused the certificate ({resp.status_code}): {resp.text}")
    attrs = resp.json()["data"]["attributes"]
    cert = x509.load_der_x509_certificate(base64.b64decode(attrs["certificateContent"]))
    print(f"Created {attrs['name']} (serial {attrs['serialNumber']}), expires {attrs['expirationDate']}")

    password = secrets.token_urlsafe(24)
    # Legacy 3DES/SHA1 encryption: macOS `security import` rejects OpenSSL 3's default AES p12.
    encryption = (
        serialization.PrivateFormat.PKCS12.encryption_builder()
        .kdf_rounds(50000)
        .key_cert_algorithm(pkcs12.PBES.PBESv1SHA1And3KeyTripleDESCBC)
        .hmac_hash(hashes.SHA1())
        .build(password.encode())
    )
    p12 = pkcs12.serialize_key_and_certificates(b"Panorama CI", key, cert, None, encryption)
    (backup / "panorama-distribution.p12").write_bytes(p12)
    (backup / "panorama-distribution-password.txt").write_text(password + "\n")
    print(f"Backup written to {backup}")

    set_secret("ASC_KEY_ID", key_id)
    set_secret("ASC_ISSUER_ID", issuer)
    set_secret("ASC_KEY_P8", p8)
    set_secret("DIST_CERT_P12_B64", base64.b64encode(p12).decode())
    set_secret("DIST_CERT_PASSWORD", password)
    set_secret("GOOGLE_SERVICE_INFO_PLIST_B64", base64.b64encode(PLIST.read_bytes()).decode())
    print("Done. Run the workflow: gh workflow run ios-testflight.yml --ref <branch>")


if __name__ == "__main__":
    if len(sys.argv) != 5:
        sys.exit(__doc__)
    main(*sys.argv[1:])
