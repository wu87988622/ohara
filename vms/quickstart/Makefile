CACHE_DIR=.cache
PACKER_CACHE_DIR=$(CACHE_DIR)/packer_cache
PACKER_LOG=1
PACKER_LOG_PATH=$(CACHE_DIR)/packer.log
BUILD_TIME=`date +'%Y/%m/%d %H:%k'`
VM_NAME=ohara-quickstart-$(OHARA_VER)

ifndef OHARA_VER
$(warning Usage:)
$(warning $(  )  $$ make OHARA_VER={version} {command})
$(error OHARA_VER required)
endif

help:
	@echo "Usage:"
	@echo "  \044 make OHARA_VER={version} {command} "
	@echo "  Both {version} and {command} is required."
	@echo "Command:"
	@echo "  clean: Remove following files:"
	@echo "         build/, ${PACKER_CACHE_DIR}/, ${PACKER_LOG_PATH}"
	@echo "  ova: Generate the OVA file. "
	@echo "       The output is build/ohara-quickstart-{OHARA_VER}.ova"
	@echo "  vm-import: Import the ova file into VirtualBox"
	@echo "  vm-start: Start quickstart VM"
	@echo "  vm-poweroff: Poweroff quickstart VM"
	@echo "  vm-reset: Reset quickstart VM"
	@echo "  vm-delete: Unregister & delete quickstart VM"
	@echo ""

clean:
	@rm -rf build
	@rm -rf $(PACKER_CACHE_DIR)
	@rm -f $(PACKER_LOG_PATH)

ova:
	@mkdir -p $(CACHE_DIR)
	@echo "OHARA_VER=$(OHARA_VER)"
	@echo "Build time: $(BUILD_TIME)"
	@echo "Start building quickstart VM ova file..."
	@PACKER_CACHE_DIR=$(PACKER_CACHE_DIR) \
	  PACKER_LOG=1 \
	  PACKER_LOG_PATH=$(PACKER_LOG_PATH) \
	  packer build -var "BUILD_TIME=$(BUILD_TIME)" -var "OHARA_VER=$(OHARA_VER)" \
	  	packer-quickstart.json
	@echo "Done."

vm-import:
	vboxmanage import build/$(VM_NAME).ova --vsys 0 --vmname $(VM_NAME)

vm-start:
	vboxmanage startvm $(VM_NAME)

vm-poweroff:
	vboxmanage controlvm $(VM_NAME) poweroff

vm-delete:
	vboxmanage unregistervm $(VM_NAME) --delete

vm-reset:
	vboxmanage controlvm $(VM_NAME) reset

