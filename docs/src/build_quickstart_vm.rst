..
.. Copyright 2019 is-land
..
.. Licensed under the Apache License, Version 2.0 (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
..     http://www.apache.org/licenses/LICENSE-2.0
..
.. Unless required by applicable law or agreed to in writing, software
.. distributed under the License is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
..

.. _build_quickstart_vm:

How to build Quickstart VM
==========================

Prerequisites
-------------

- Operation system: Linux
- Make_: Make is a build automation tool that automatically builds executable programs.
  **Make** is already built in Linux or macOS.
- Packer_ 1.4+: Packer is an open source tool for creating identical machine images
  for multiple platforms from a single source configuration.
- VirtualBox_ 5.2+


Build OVA file
--------------

- We use **make** command to execute all our tasks. For building the specific version of Ohara quickstart VM, you must provide the argument **OHARA_VER**
  when execute the **make** command.

Build the OVA file, following is an example(OHARA_VER=0.7.1):

  .. code-block:: console

    [ohara]$ cd vms/quickstart
    [quickstart]$ make OHARA_VER=0.7.1 ova
    OHARA_VER=0.7.1
    Build time: 2019/09/10 10:10
    Start building quickstart VM ova file...
    virtualbox-iso output will be in this color.

    ==> virtualbox-iso: Retrieving ISO
    ==> virtualbox-iso: Trying .cache/ubuntu-18.04.3-server-amd64.iso
    ==> virtualbox-iso: Trying .cache/ubuntu-18.04.3-server-amd64.iso?checksum=sha256%3A7d8e0055d663bffa27c1718685085626cb59346e7626ba3d3f476322271f573e
    ==> virtualbox-iso: .cache/ubuntu-18.04.3-server-amd64.iso?checksum=sha256%3A7d8e0055d663bffa27c1718685085626cb59346e7626ba3d3f476322271f573e => /your/project/path/ohara/vms/quickstart/.cache/packer_cache/fdcf467e727a368c2aac26ac2284f0f517dc29fb.iso
    ==> virtualbox-iso: Starting HTTP server on port 8251
    ==> virtualbox-iso: Creating virtual machine...
    ==> virtualbox-iso: Creating hard drive...
    ==> virtualbox-iso: Creating forwarded port mapping for communicator (SSH, WinRM, etc) (host port 3248)
    ==> virtualbox-iso: Executing custom VBoxManage commands...
    :   :   :   :
    (SKIP)
    :   :   :   :
    ==> virtualbox-iso: Gracefully halting virtual machine...
    ==> virtualbox-iso: Preparing to export machine...
        virtualbox-iso: Deleting forwarded port mapping for the communicator (SSH, WinRM, etc) (host port 3248)
    ==> virtualbox-iso: Exporting virtual machine...
        virtualbox-iso: Executing: export ohara-quickstart-0.7.1 --output build/ohara-quickstart-0.7.1.ova
    ==> virtualbox-iso: Deregistering and deleting VM...
    Build 'virtualbox-iso' finished.

    ==> Builds finished. The artifacts of successful builds are:
    --> virtualbox-iso: VM files in directory: build
    Done.

The OVA file will be output to: build/ohara-quickstart-{OHARA_VER}.ova


Import OVA
----------

After generated the quickstart ova file,
you can use VirtualBox user interface to import the OVA file(**File -> Import Appliance**)
or use following command:

  .. code-block:: console

    [quickstart]$ make OHARA_VER=0.7.1 vm-import
    vboxmanage import build/ohara-quickstart-0.7.1.ova --vsys 0 --vmname ohara-quickstart-0.7.1
    0%...10%...20%...30%...40%...50%...60%...70%...80%...90%...100%
    Interpreting /your/project/path/ohara/vms/quickstart/build/ohara-quickstart-0.7.1.ova...
    OK.
    Disks:
      vmdisk1       85899345920     -1      http://www.vmware.com/interfaces/specifications/vmdk.html#streamOptimized       ohara-quickstart-0.7.1-disk001.vmdk -1      -1

    Virtual system 0:
     0: Suggested OS type: "Ubuntu_64"
        (change with "--vsys 0 --ostype <type>"; use "list ostypes" to list all possible values)
     1: VM name specified with --vmname: "ohara-quickstart-0.7.1"
     2: Description "Ohara Quickstart VM
    Ohara version: 0.7.1
    Build time: 2019/09/10 10:10"
        (change with "--vsys 0 --description <desc>")
     3: Number of CPUs: 2
        (change with "--vsys 0 --cpus <n>")
     4: Guest memory: 4096 MB
        (change with "--vsys 0 --memory <MB>")
     5: Network adapter: orig NAT, config 3, extra slot=0;type=NAT
     6: Network adapter: orig HostOnly, config 3, extra slot=1;type=HostOnly
     7: IDE controller, type PIIX4
        (disable with "--vsys 0 --unit 7 --ignore")
     8: IDE controller, type PIIX4
        (disable with "--vsys 0 --unit 8 --ignore")
     9: Hard disk image: source image=ohara-quickstart-0.7.1-disk001.vmdk, target path=/home/xxxx/VirtualBox VMs/ohara-quickstart-0.7.1/ohara-quickstart-0.7.1-disk001.vmdk, controller=7;channel=0
        (change target path with "--vsys 0 --unit 9 --disk path";
        disable with "--vsys 0 --unit 9 --ignore")


Use Quickstart VM
-----------------

After import quickstart VM to VirtualBox, you can press **Start** button to start the VM.
And then you can see following screen:

  .. code-block:: console

    Ubuntu 10.04.03 LTS ohara-vm tty1
    ohara-vm login:

Please use ``ohara`` as login account and ``oharastream`` as password to login to VM.
If this is your first time to login Quickstart VM, the progress of pull Ohara docker
images will be starting automatically.
So please make sure your machine can connect to Internet.

After download the images, and then you can see the ip address info of the VM, for example:

  .. code-block:: text

    IP address info:
    lo              UNKNOWN         127.0.0.1/8 ::1/128
    enp0s3          UP              10.0.2.15/24 fe80::a00:27ff:feac:ad8a/64
    enp0s8          UP              192.168.56.114/24 fe80::a00:27ff:fe09:1a1e/64
    docker0         DOWN            172.17.0.1/16

We can find the private IP address **192.168.56.114** (enp0s8) in the above list.
So the configurator ip address is **192.168.56.114** .

Run Ohara configurator(port 12345):
  .. code-block:: console

    $ ./ohara-configurator.sh
    + docker run --rm -p 12345:12345 -d oharastream/configurator:0.7.1 --port 12345

Run Ohara manager(port 5050), provide the configurator ip address as parameter:
  .. code-block:: console

    $ ./ohara-manager.sh 192.168.56.114
    + docker run --rm -p 5050:5050 -d oharastream/manager:0.7.1 --port 5050 --configurator http://192.168.56.114:12345/v0

Now you can open your browser and input the link: http://192.168.56.114:5050
to open the main page of Ohara Manager.


Other commands
--------------

Following are other commands for development purpose:

  .. code-block:: console

    [quickstart]$ make OHARA_VER=0.7.1
    Usage:
      $ make OHARA_VER={version} {command}
      Both {version} and {command} is required.
    Command:
      clean: Remove following files:
             build/, .cache/packer_cache/, .cache/packer.log
      ova: Generate the OVA file.
           The output is build/ohara-quickstart-{OHARA_VER}.ova
      vm-import: Import the ova file into VirtualBox
      vm-start: Start quickstart VM
      vm-poweroff: Poweroff quickstart VM
      vm-reset: Reset quickstart VM
      vm-delete: Unregister & delete quickstart VM



.. _Packer: https://www.packer.io/
.. _Make: https://en.wikipedia.org/wiki/Make_(software)
.. _VirtualBox: https://www.virtualbox.org/
