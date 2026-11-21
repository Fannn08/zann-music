#!/usr/bin/env bash
# Rilis ZANN MUSIC: ./rilis.sh 1.9.22   (tambah -y untuk lewati konfirmasi)
set -euo pipefail

VER="${1:-}"
YES="${2:-}"
cd "$(dirname "$0")"

die() { echo "GAGAL: $*" >&2; exit 1; }

[[ "$VER" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "pakai: ./rilis.sh 1.9.22"
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die "bukan folder repo git"
command -v gh >/dev/null || die "gh belum terpasang"
gh auth status >/dev/null 2>&1 || die "gh belum login (jalankan: gh auth login)"

GRADLE=app/build.gradle
[ -f "$GRADLE" ] || die "$GRADLE tidak ada"
[ -f CHANGELOG.md ] || die "CHANGELOG.md tidak ada"

CUR_NAME=$(sed -nE "s/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*['\"]([^'\"]+)['\"].*/\1/p" "$GRADLE" | head -1)
CUR_CODE=$(sed -nE "s/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p" "$GRADLE" | head -1)
[ -n "$CUR_NAME" ] && [ -n "$CUR_CODE" ] || die "versionName/versionCode tidak terbaca di $GRADLE"
NEW_CODE=$((CUR_CODE + 1))
BRANCH=$(git rev-parse --abbrev-ref HEAD)

# versi baru harus lebih tinggi dari yang sekarang
HIGHEST=$(printf '%s\n%s\n' "$CUR_NAME" "$VER" | sort -V | tail -1)
{ [ "$HIGHEST" = "$VER" ] && [ "$CUR_NAME" != "$VER" ]; } || die "versi $VER harus lebih tinggi dari $CUR_NAME"

# catatan versi harus sudah ditulis
grep -qE "^## ${VER//./\\.}[[:space:]]*$" CHANGELOG.md || die "tulis dulu catatan di CHANGELOG.md dengan judul: ## $VER"

# tag belum boleh ada
git rev-parse -q --verify "refs/tags/v$VER" >/dev/null && die "tag v$VER sudah ada di lokal"
git ls-remote --exit-code --tags origin "refs/tags/v$VER" >/dev/null 2>&1 && die "tag v$VER sudah ada di GitHub"

# file kunci tidak boleh ikut (cek sebelum mengubah apa pun)
if git status --porcelain | cut -c4- | grep -Ei '(keystore|\.jks$|\.p12$|\.pfx$)'; then
  die "file kunci akan ikut ter-commit (daftar di atas). Tambahkan ke .gitignore dulu."
fi

echo "Rilis ZANN MUSIC"
echo "  versi     : $CUR_NAME -> $VER"
echo "  versionCode: $CUR_CODE -> $NEW_CODE"
echo "  cabang    : $BRANCH"
echo "  tag       : v$VER"
if [ "$YES" != "-y" ]; then
  read -r -p "Lanjut? (y/n) " ans
  [ "$ans" = "y" ] || die "dibatalkan"
fi

sed -i -E "s/^([[:space:]]*versionCode[[:space:]]*=[[:space:]]*)[0-9]+/\1${NEW_CODE}/" "$GRADLE"
sed -i -E "s/^([[:space:]]*versionName[[:space:]]*=[[:space:]]*)(['\"])[^'\"]*(['\"])/\1\2${VER}\3/" "$GRADLE"

git add -A
# jangan pernah ikutkan kunci tanda tangan
if git diff --cached --name-only | grep -Ei '(keystore|\.jks$|\.p12$|\.pfx$)'; then
  git reset -q
  die "file kunci ikut ter-stage (daftar di atas). Dibatalkan; tambahkan ke .gitignore."
fi

git commit -q -m "Rilis $VER"
git push origin "$BRANCH"
git tag "v$VER"
git push origin "v$VER"

echo "Tag v$VER terkirim. Menunggu build rilis..."
sleep 8
RUN=$(gh run list --workflow=release.yml --limit 1 --json databaseId -q '.[0].databaseId')
if gh run watch "$RUN" --exit-status; then
  echo "SELESAI: rilis v$VER terbit. Lihat: gh release view v$VER"
else
  echo "Build rilis gagal. Lihat: gh run view $RUN --log-failed" >&2
  exit 1
fi
