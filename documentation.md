# Arch Linux Assignment Documentation
*Sydney Tran - CYB3353 System Administration*

## Overview
This document provides a summary of all the steps I took in completing the Arch Linux Installation Project.

## Setting Up the Virtual Machine (VM)

I started this project off by downloading umd.edu's ISO, `archlinux-2024.10.01-x86_64.iso` from https://archlinux.org/download/. 

Using VirtualBox, I created a new VM with the following:
- Type: Arch Linux (64-bit)
- Memory: 20 GB
- Storage: New virtual hard disk with 20 GB

To ensure my network interface is listed and enabled, I used `ip link` and I verified my network connection with `ping archlinux.org`.

Further more, with the command `timedatectl`, I was able to ensure the system clock is synchronized. 

## Disk Partitioning and Formatting 

### List Disks:
- Used the command `fdisk -l` to see the available disks.


### Creating a Root Partition 
- Opened `fdisk` on `/dev/sda` with `fdisk /dev/sda`
- Created the root partition through the following:
     - Typed `n` to get a new partition
    - Typed `p` for primary partition
    - Chose `1` for the first partition
    - Chose the default sectors
- Formatted the root partition using `mkfs.ext4 /dev/sda1`
- Mounted the root partition with `mount /dev/sda1 /mnt`

### Base Installation and System Setup
- Installed base packages with `pacstrap /mnt base linux linux-firmware`
- Generated the filesystem table with `genfstab -U /mnt >> /mnt/etc/fstab`
- Entered the chroot into the new system with `arch-chroot /mnt`. 

### System Configuration Inside Chroot
Time Zone Setup:
- Updated the time zone with `ln -sf /usr/share/zoneinfo/America/Chicago /etc/localtime`
- Ran `hwclock --systohc` to set the hardware clock

Localization:
- Generated the locales by running `locale-gen`
- Created a locale configuration file with `echo "LANG=en_US.UTF-8" > /etc/locale.conf`

Hostname:
- Created a hostname file called "gato" with the command `echo "gato" > /etc/hostname`

Install Additional Packages:
- Installed core tools and network utilities with  `pacman -Syu networkmanager nano openssh`
- Enabled SSH service with `sudo systemctl enable sshd`
- Started SSH service with `sudo systemctl start ssh`
- Enabled NetworkManager with `systemctl enable NetworkManager`

### User and Sudo Configuration
- Created users `sydney`, `justin`, and `codi` with `useradd -m -G wheel [username]`
- Set each user's password to `GraceHopper1906` using the `passwd` command
- Installed sudo with `pacman -S sudo`
- Then ran `usermod -aG wheel [username]` for each user to give sudo priviledges
- Edited the `sudoers` file with `EDITOR=nano visudo` and uncommended `%wheel ALL=(ALL) ALL` to allow the wheel group the run sudo commands

### Desktop Environment and Display Manager Setup
- I used `pacman -S lxqt` to install Desktop Environment (LXQT) 
- Installed LightDM with `pacman -S lightdm lightdm-gtk-greeter`
- Enabled LightDM with `systemctl enable lightdm`

## Additional Software and Customization
- Installed `zsh` with `pacman -S zsh`
- Installed Firefox with `sudo pacman -S firefox`
- Added color to the terminal with `echo "export TERM=xterm-256color" >> ~/.bashrc`
- Configured useful aliases by editing `~/.bashrc` with the command, `nano ~/.bashrc`: 
    - `alias ll='ls -l --color=auto'`
    - `alias l='ls --color=auto'`
    - `alias sm='echo "you got this!"'`
- Loaded these aliases with `source ~/.bashrc`
- Did these exact steps with `~/.zshrc` as well. 

### Bootloader Installation (GRUB)
- Used https://wiki.archlinux.org/title/GRUB to help install GRUB
- Installed GRUB with `pacman -S grub`
- Installed GRUB on the primary disk with `grub-install --target=i386-pc /dev/sda`
- Generated the GRUB configuration file with `grub-mkconfig -o /boot/grub/grub.cfg`

### Finishing Up
- Set the root password using `passwd`
- Exit the chroot engvironment with `exit`
- Unmounted partitions with `umount -R /mnt` 
- Rebooted the system with `reboot`