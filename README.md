# Kubernetes offline docs APK packager

Requirements
* make
* pagefind
* docker
* git
* jdk 21
* python3

Build and install a release:

    RELEASE=1.37 make install

If `release-1.37/` does not exist, `make` clones the matching upstream branch automatically.
If it already exists, it is reused.

With multiple ADB devices connected:

    RELEASE=1.37 ADB_SERIAL=<serial> make install

Useful targets:

    RELEASE=1.37 make build
    RELEASE=1.37 make update
    RELEASE=1.37 make clean
    RELEASE=1.37 make clean-release
    RELEASE=1.37 make clean-all

Environment overrides:

    ANDROID_HOME=...
    JAVA_HOME=...
    CONTAINER_ENGINE=podman
    K8S_DOCS_SEGMENT=en
    K8S_DOCS_BASE_URL=https://k8s.local/
    GRADLE_VERSION=8.9

The generated APK is written to:

    dist/k8s-docs-v1.37.apk

Hugo output and Pagefind indexes are cached per release under `.build-cache/release-X.Y/`.
Vendored third-party runtime assets are cached globally under `.build-cache/vendor/`.
