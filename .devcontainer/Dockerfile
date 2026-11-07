FROM mcr.microsoft.com/devcontainers/java:25

ARG TYPOS_VERSION=1.52.0
ARG TBLS_VERSION=1.96.1

RUN set -eu; \
    case "$(uname -m)" in \
      aarch64) \
        typos_arch=aarch64; typos_sha=63b7d4478d467a61fe47cdb3162f720c6797856e61a04f7e125d1cdc282488b8; \
        tbls_arch=arm64; tbls_sha=f5da6e28b787ca934326870f7b69812c76e097d35a8ae612533108060f76850a ;; \
      x86_64) \
        typos_arch=x86_64; typos_sha=6155d4114f9a0f5b8f6d4fda96266c9d5f6add71df80da7f684ee5e72eb14588; \
        tbls_arch=amd64; tbls_sha=18285eef0ba7d917214e8bd7dd6a22afe5fea021eed52e7149100a3d10a2af95 ;; \
      *) echo "Unsupported architecture: $(uname -m)" >&2; exit 1 ;; \
    esac; \
    install_tool() { \
      curl -fsSL -o /tmp/tool.tar.gz "$1"; \
      echo "$2  /tmp/tool.tar.gz" | sha256sum -c -; \
      tar -xzf /tmp/tool.tar.gz -C /usr/local/bin "$3"; \
      rm /tmp/tool.tar.gz; \
    }; \
    install_tool "https://github.com/crate-ci/typos/releases/download/v${TYPOS_VERSION}/typos-v${TYPOS_VERSION}-${typos_arch}-unknown-linux-musl.tar.gz" "$typos_sha" ./typos; \
    install_tool "https://github.com/k1LoW/tbls/releases/download/v${TBLS_VERSION}/tbls_v${TBLS_VERSION}_linux_${tbls_arch}.tar.gz" "$tbls_sha" tbls
