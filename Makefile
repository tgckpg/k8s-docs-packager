RELEASE ?=
WEBSITE_URL ?= https://github.com/kubernetes/website.git
WEBSITE_DIR = release-$(RELEASE)
APK = dist/k8s-docs-v$(RELEASE).apk
ADB ?= adb
ADB_SERIAL ?=
ADB_ARGS = $(if $(strip $(ADB_SERIAL)),-s $(ADB_SERIAL),)

ifeq ($(strip $(RELEASE)),)
$(error RELEASE is required, e.g. RELEASE=1.37 make install)
endif

.PHONY: all repo build apk install update clean clean-release clean-all

all: build

repo:
	@if [ ! -d "$(WEBSITE_DIR)/.git" ]; then \
		if [ -e "$(WEBSITE_DIR)" ]; then \
			echo "error: $(WEBSITE_DIR) exists but is not a git checkout" >&2; \
			exit 1; \
		fi; \
		echo "== Cloning Kubernetes website release-$(RELEASE) =="; \
		git clone \
			--branch "release-$(RELEASE)" \
			--single-branch \
			--depth 1 \
			--recurse-submodules \
			"$(WEBSITE_URL)" \
			"$(WEBSITE_DIR)"; \
	fi
	@git -C "$(WEBSITE_DIR)" submodule update --init --recursive

build apk: repo
	@RELEASE="$(RELEASE)" ./src/build

install: build
	@$(ADB) $(ADB_ARGS) install -r "$(APK)"

update: repo
	@git -C "$(WEBSITE_DIR)" pull --ff-only
	@git -C "$(WEBSITE_DIR)" submodule update --init --recursive

clean:
	@rm -rf "src/android/app/build" "src/android/.gradle" "dist/k8s-docs-v$(RELEASE).apk"

clean-release:
	@rm -rf ".build-cache/release-$(RELEASE)"

clean-all:
	@rm -rf .build-cache .build-tools dist src/android/app/build src/android/.gradle
