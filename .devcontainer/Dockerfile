FROM mcr.microsoft.com/devcontainers/java:25

ARG TYPOS_VERSION=1.52.0

RUN set -eu; \
    case "$(uname -m)" in \
      aarch64) arch=aarch64; sha=63b7d4478d467a61fe47cdb3162f720c6797856e61a04f7e125d1cdc282488b8 ;; \
      x86_64) arch=x86_64; sha=6155d4114f9a0f5b8f6d4fda96266c9d5f6add71df80da7f684ee5e72eb14588 ;; \
      *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;; \
    esac; \
    file="typos-v${TYPOS_VERSION}-${arch}-unknown-linux-musl.tar.gz"; \
    curl -fsSL -o "/tmp/${file}" "https://github.com/crate-ci/typos/releases/download/v${TYPOS_VERSION}/${file}"; \
    echo "${sha}  /tmp/${file}" | sha256sum -c -; \
    tar -xzf "/tmp/${file}" -C /usr/local/bin ./typos; \
    rm "/tmp/${file}"
