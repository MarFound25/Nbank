#!/usr/bin/env bash
set -euo pipefail

VERSION="${SWAGGER_COVERAGE_VERSION:-1.5.0}"
ZIP_NAME="swagger-coverage-${VERSION}.zip"
DIR=".swagger-coverage-commandline"
API_DOCS_URL="${SWAGGER_API_DOCS_URL:-http://localhost:4112/v3/api-docs}"
INPUT_DIR="${SWAGGER_COVERAGE_INPUT:-target/swagger-coverage-output}"

if [[ ! -x "${DIR}/bin/swagger-coverage-commandline" ]]; then
  echo ">>> Downloading swagger-coverage ${VERSION}"
  curl -sL -o "${ZIP_NAME}" \
    "https://github.com/viclovsky/swagger-coverage/releases/download/${VERSION}/${ZIP_NAME}"
  rm -rf "${DIR}"
  unzip -q "${ZIP_NAME}"
  # Zip root folder name may vary; normalize to .swagger-coverage-commandline
  EXTRACTED="$(find . -maxdepth 1 -type d -name 'swagger-coverage*' ! -name '.' | head -n 1)"
  if [[ -n "${EXTRACTED}" && "${EXTRACTED}" != "./${DIR}" ]]; then
    mv "${EXTRACTED}" "${DIR}"
  fi
  rm -f "${ZIP_NAME}"
  chmod +x "${DIR}/bin/swagger-coverage-commandline" || true
fi

if [[ ! -d "${INPUT_DIR}" ]]; then
  echo "Coverage input directory not found: ${INPUT_DIR}"
  echo "Run API tests first so RestAssured writes coverage files."
  exit 1
fi

echo ">>> Generating Swagger coverage report"
echo "    spec:  ${API_DOCS_URL}"
echo "    input: ${INPUT_DIR}"
"${DIR}/bin/swagger-coverage-commandline" -s "${API_DOCS_URL}" -i "${INPUT_DIR}"
echo ">>> Done. Open swagger-coverage-report.html"
